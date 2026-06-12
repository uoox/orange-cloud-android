//
//  Zone.swift
//  Orange Cloud
//

import Foundation

nonisolated struct Zone: Codable, Identifiable, Hashable, Sendable {
    let id:          String
    let name:        String
    let status:      String          // "active" | "pending" | "paused" 等
    let plan:        ZonePlan?
    let nameServers: [String]?

    enum CodingKeys: String, CodingKey {
        case id, name, status, plan
        case nameServers = "name_servers"
    }
}

nonisolated struct ZonePlan: Codable, Hashable, Sendable {
    let name: String
}
