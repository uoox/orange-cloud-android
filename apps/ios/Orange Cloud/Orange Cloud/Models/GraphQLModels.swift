//
//  GraphQLModels.swift
//  Orange Cloud
//
//  GraphQL Analytics API 的通用请求/响应信封（与 REST 的 CFAPIResponse 不同）。
//

import Foundation

nonisolated struct GraphQLRequest<V: Codable & Sendable>: Codable, Sendable {
    let query:     String
    let variables: V
}

nonisolated struct GraphQLResponse<D: Codable & Sendable>: Codable, Sendable {
    let data:   D?
    let errors: [GraphQLError]?
}

nonisolated struct GraphQLError: Codable, Sendable {
    let message: String
}
