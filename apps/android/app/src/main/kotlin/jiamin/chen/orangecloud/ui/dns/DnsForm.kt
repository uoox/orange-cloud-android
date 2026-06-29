package jiamin.chen.orangecloud.ui.dns

/** DNS 编辑表单的领域选项与规则（与 iOS DNSRecordFormView 对齐）。 */
object DnsForm {
    /** 支持编辑的记录类型。 */
    val recordTypes = listOf("A", "AAAA", "CNAME", "TXT", "MX", "NS")

    /** TTL 秒值；1 = 自动。标签在 UI 层按值映射 strings.xml。 */
    val ttlValues = listOf(1, 60, 300, 1800, 3600, 86400)

    /** 只有 A / AAAA / CNAME 支持 Cloudflare 代理。 */
    fun supportsProxy(type: String): Boolean = type == "A" || type == "AAAA" || type == "CNAME"

    /** 仅 MX 需要优先级。 */
    fun needsPriority(type: String): Boolean = type == "MX"

    /** 内容是否满足该记录类型的最小格式（A→IPv4、AAAA→IPv6，其余仅非空），避免提交明显非法记录。 */
    fun isContentValid(type: String, content: String): Boolean {
        val c = content.trim()
        if (c.isEmpty()) return false
        return when (type) {
            "A" -> isIPv4(c)
            "AAAA" -> isIPv6(c)
            else -> true
        }
    }

    private fun isIPv4(s: String): Boolean {
        val parts = s.split(".")
        if (parts.size != 4) return false
        return parts.all { p ->
            // 每段 0–255，且不允许 "01" 这类前导零（"0" 本身合法）
            p.length in 1..3 && p.all { it.isDigit() } &&
                p.toInt() in 0..255 && (p.length == 1 || p[0] != '0')
        }
    }

    private fun isIPv6(s: String): Boolean {
        if (!s.contains(":")) return false
        // 仅允许十六进制段、冒号与（IPv4 内嵌的）点；最多一个 "::" 压缩
        if (!s.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' }) return false
        if (s.split("::").size > 2) return false
        return s.split(":").size in 3..8
    }
}
