//
//  TunnelModels.swift
//  Orange Cloud
//
//  Cloudflare Tunnel（cfd_tunnel）。GET /accounts/{id}/cfd_tunnel
//  列表响应已内嵌活跃连接，无需单独拉取。
//

import Foundation

nonisolated struct Tunnel: Codable, Identifiable, Hashable, Sendable {
    let id:            String
    let name:          String
    let status:        String?            // "inactive" | "degraded" | "healthy" | "down"
    let createdAt:     String?
    let connsActiveAt: String?
    let tunType:       String?            // "cfd_tunnel" | "warp_connector" ...
    let remoteConfig:  Bool?
    let connections:   [TunnelConnection]?

    enum CodingKeys: String, CodingKey {
        case id, name, status, connections
        case createdAt     = "created_at"
        case connsActiveAt = "conns_active_at"
        case tunType       = "tun_type"
        case remoteConfig  = "remote_config"
    }

    var statusText: String {
        switch status {
        case "healthy":  String(localized: "运行中")
        case "degraded": String(localized: "降级")
        case "down":     String(localized: "离线")
        case "inactive": String(localized: "未激活")
        default:         status ?? String(localized: "未知")
        }
    }
}

nonisolated struct TunnelConnection: Codable, Hashable, Sendable {
    let id:            String?
    let coloName:      String?
    let originIp:      String?
    let openedAt:      String?
    let clientVersion: String?

    enum CodingKeys: String, CodingKey {
        case id
        case coloName      = "colo_name"
        case originIp      = "origin_ip"
        case openedAt      = "opened_at"
        case clientVersion = "client_version"
    }
}
