//
//  TunnelService.swift
//  Orange Cloud
//

import Foundation

struct TunnelService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 账号下全部 Tunnel（排除已删除，页码分页）
    func listTunnels(accountId: String) async throws -> [Tunnel] {
        var tunnels: [Tunnel] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<Tunnel> = try await client.get(
                "accounts/\(accountId)/cfd_tunnel",
                queryItems: [
                    URLQueryItem(name: "is_deleted", value: "false"),
                    URLQueryItem(name: "page",       value: String(page)),
                    URLQueryItem(name: "per_page",   value: "100"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            tunnels.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return tunnels
    }
}
