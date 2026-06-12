//
//  ZoneSettingsModels.swift
//  Orange Cloud
//
//  Zone 设置（security_level / development_mode）与缓存清理。
//

import Foundation

/// GET/PATCH /zones/{id}/settings/{setting} 的 result
nonisolated struct ZoneSetting: Codable, Sendable {
    let id:    String?
    let value: String
}

nonisolated struct ZoneSettingUpdate: Codable, Sendable {
    let value: String
}

/// POST /zones/{id}/purge_cache
nonisolated struct PurgeRequest: Codable, Sendable {
    let purgeEverything: Bool

    enum CodingKeys: String, CodingKey {
        case purgeEverything = "purge_everything"
    }
}

nonisolated struct PurgeResult: Codable, Sendable {
    let id: String?
}
