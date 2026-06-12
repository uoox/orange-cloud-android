//
//  ZoneEntity.swift
//  Orange Cloud
//
//  Zone 的 App Intents 实体：数据来自 SwiftData 本地缓存，Siri/快捷指令可离线查询。
//

import Foundation
import AppIntents
import SwiftData

nonisolated struct ZoneEntity: AppEntity, Identifiable {

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "域名"
    static let defaultQuery = ZoneEntityQuery()

    let id:       String
    let name:     String
    let status:   String
    let planName: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(name)",
            subtitle: "\(status) · \(planName)"
        )
    }

    @MainActor
    init(from cached: CachedZone) {
        self.id       = cached.id
        self.name     = cached.name
        self.status   = cached.status
        self.planName = cached.planName
    }
}

nonisolated struct ZoneEntityQuery: EntityQuery {

    func entities(for identifiers: [ZoneEntity.ID]) async throws -> [ZoneEntity] {
        try await MainActor.run {
            let context = ModelContext(CacheContainer.shared)
            let zones = try context.fetch(FetchDescriptor<CachedZone>())
            return zones
                .filter { identifiers.contains($0.id) }
                .map(ZoneEntity.init(from:))
        }
    }

    func suggestedEntities() async throws -> [ZoneEntity] {
        try await MainActor.run {
            let context = ModelContext(CacheContainer.shared)
            let zones = try context.fetch(
                FetchDescriptor<CachedZone>(sortBy: [SortDescriptor(\.name)])
            )
            return zones.map(ZoneEntity.init(from:))
        }
    }
}
