//
//  ZoneService.swift
//  Orange Cloud
//

import Foundation

struct ZoneService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 拉取账号下全部 Zone（自动翻页）
    func listZones(accountId: String) async throws -> [Zone] {
        var zones: [Zone] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<Zone> = try await client.get(
                "zones",
                queryItems: [
                    URLQueryItem(name: "account.id", value: accountId),
                    URLQueryItem(name: "page",       value: String(page)),
                    URLQueryItem(name: "per_page",   value: "50"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            zones.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return zones
    }

    func getZone(zoneId: String) async throws -> Zone {
        let response: CFAPIResponse<Zone> = try await client.get("zones/\(zoneId)")
        guard response.success, let zone = response.result else {
            throw response.toAPIError()
        }
        return zone
    }
}
