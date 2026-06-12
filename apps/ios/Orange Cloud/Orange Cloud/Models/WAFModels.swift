//
//  WAFModels.swift
//  Orange Cloud
//
//  WAF 自定义规则（Rulesets API，phase = http_request_firewall_custom）。
//  GET /zones/{id}/rulesets/phases/http_request_firewall_custom/entrypoint
//

import Foundation

nonisolated struct WAFRuleset: Codable, Sendable {
    let id:    String
    let name:  String?
    let phase: String?
    let rules: [WAFRule]?
}

nonisolated struct WAFRule: Codable, Identifiable, Hashable, Sendable {
    let id:          String
    let action:      String?         // "block" | "challenge" | "managed_challenge" | "js_challenge" | "log" | "skip"
    let expression:  String?
    let description: String?
    let enabled:     Bool?
    let lastUpdated: String?

    enum CodingKeys: String, CodingKey {
        case id, action, expression, description, enabled
        case lastUpdated = "last_updated"
    }

    var actionText: String {
        switch action {
        case "block":             String(localized: "拦截")
        case "challenge":         String(localized: "质询")
        case "managed_challenge": String(localized: "托管质询")
        case "js_challenge":      String(localized: "JS 质询")
        case "log":               String(localized: "记录")
        case "skip":              String(localized: "跳过")
        case "allow":             String(localized: "放行")
        default:                  action ?? "—"
        }
    }
}

/// PATCH 规则只更新 enabled
nonisolated struct WAFRuleToggle: Codable, Sendable {
    let enabled: Bool
}

/// 新建规则（POST rules / PUT entrypoint 共用）
nonisolated struct WAFRuleCreate: Codable, Sendable {
    let action:      String
    let expression:  String
    let description: String?
    let enabled:     Bool
}

/// PUT entrypoint 创建规则集（Zone 首条自定义规则时）
nonisolated struct WAFEntrypointUpdate: Codable, Sendable {
    let rules: [WAFRuleCreate]
}

/// 自定义规则可用的动作（skip 需要额外参数，暂不提供）
nonisolated enum WAFRuleAction: String, CaseIterable, Identifiable, Sendable {
    case block
    case managedChallenge = "managed_challenge"
    case jsChallenge      = "js_challenge"
    case challenge
    case log

    var id: String { rawValue }

    var label: String {
        switch self {
        case .block:            String(localized: "拦截")
        case .managedChallenge: String(localized: "托管质询")
        case .jsChallenge:      String(localized: "JS 质询")
        case .challenge:        String(localized: "质询")
        case .log:              String(localized: "仅记录")
        }
    }
}
