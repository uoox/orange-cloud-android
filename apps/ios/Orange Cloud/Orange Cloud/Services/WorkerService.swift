//
//  WorkerService.swift
//  Orange Cloud
//

import Foundation

struct WorkerService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 账号下全部 Workers 脚本（该端点不分页）
    func listScripts(accountId: String) async throws -> [WorkerScript] {
        let response: CFAPIResponseArray<WorkerScript> = try await client.get(
            "accounts/\(accountId)/workers/scripts"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }
}
