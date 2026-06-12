//
//  AccountPrefsStore.swift
//  Orange Cloud
//
//  按 Cloudflare 账户（account ID）存储的偏好：账单日、套餐预设。
//  account ID 跨设备稳定，开启 iCloud 同步后经 NSUbiquitousKeyValueStore 同步。
//

import Foundation
import Observation

@Observable
@MainActor
final class AccountPrefsStore {

    static let shared = AccountPrefsStore()

    nonisolated struct Prefs: Codable, Sendable, Equatable {
        var billingCycleDay: Int = 1      // 1 = 自然月
        var workersPlanPaid: Bool = false
        var r2PlanPaid:      Bool = false
    }

    private(set) var all: [String: Prefs] = [:]

    private static let storeKey = "accountPrefsById"
    private let defaultsTemplate: Prefs

    private var syncEnabled: Bool {
        UserDefaults.standard.bool(forKey: AuthManager.iCloudSyncKey)
    }

    private init() {
        // 旧版全局偏好作为新账户的默认模板（平滑迁移）
        var template = Prefs()
        let defaults = UserDefaults.standard
        if defaults.object(forKey: "billingCycleDay") != nil {
            template.billingCycleDay = max(defaults.integer(forKey: "billingCycleDay"), 1)
        }
        template.workersPlanPaid = defaults.bool(forKey: "workersPlanPaid")
        template.r2PlanPaid = defaults.bool(forKey: "r2PlanPaid")
        defaultsTemplate = template

        if let data = defaults.data(forKey: Self.storeKey),
           let decoded = try? JSONDecoder().decode([String: Prefs].self, from: data) {
            all = decoded
        }
        mergeFromCloud()

        NotificationCenter.default.addObserver(
            forName: NSUbiquitousKeyValueStore.didChangeExternallyNotification,
            object: NSUbiquitousKeyValueStore.default,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.mergeFromCloud()
            }
        }
    }

    func prefs(for accountId: String) -> Prefs {
        all[accountId] ?? defaultsTemplate
    }

    func update(_ accountId: String, _ mutate: (inout Prefs) -> Void) {
        guard !accountId.isEmpty else { return }
        var prefs = prefs(for: accountId)
        mutate(&prefs)
        all[accountId] = prefs
        persist()
    }

    /// 同步开关变更：开启则推送 + 拉取，关闭则从云端移除
    func applySyncChange(_ enabled: Bool) {
        if enabled {
            persist()
            mergeFromCloud()
        } else {
            NSUbiquitousKeyValueStore.default.removeObject(forKey: Self.storeKey)
            NSUbiquitousKeyValueStore.default.synchronize()
        }
    }

    private func mergeFromCloud() {
        guard syncEnabled,
              let data = NSUbiquitousKeyValueStore.default.data(forKey: Self.storeKey),
              let cloud = try? JSONDecoder().decode([String: Prefs].self, from: data) else { return }
        // 按账户合并，云端较新视角优先（偏好是幂等设置，最后写入者胜出可接受）
        var changed = false
        for (accountId, prefs) in cloud where all[accountId] != prefs {
            all[accountId] = prefs
            changed = true
        }
        if changed {
            persistLocalOnly()
        }
    }

    private func persist() {
        persistLocalOnly()
        if syncEnabled, let data = try? JSONEncoder().encode(all) {
            NSUbiquitousKeyValueStore.default.set(data, forKey: Self.storeKey)
            NSUbiquitousKeyValueStore.default.synchronize()
        }
    }

    private func persistLocalOnly() {
        if let data = try? JSONEncoder().encode(all) {
            UserDefaults.standard.set(data, forKey: Self.storeKey)
            // 镜像到 App Group：Widget 自取数时需要账单日/套餐口径
            UserDefaults(suiteName: WidgetSnapshot.appGroupID)?.set(data, forKey: Self.storeKey)
        }
    }
}
