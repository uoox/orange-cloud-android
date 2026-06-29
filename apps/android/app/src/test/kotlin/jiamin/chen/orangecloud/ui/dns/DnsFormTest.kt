package jiamin.chen.orangecloud.ui.dns

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsFormTest {

    @Test
    fun aRecordAcceptsValidIPv4() {
        assertTrue(DnsForm.isContentValid("A", "1.2.3.4"))
        assertTrue(DnsForm.isContentValid("A", "192.168.0.1"))
        assertTrue(DnsForm.isContentValid("A", "0.0.0.0"))
        assertTrue(DnsForm.isContentValid("A", " 10.0.0.1 ")) // 前后空白会被裁剪
    }

    @Test
    fun aRecordRejectsInvalidIPv4() {
        assertFalse(DnsForm.isContentValid("A", "256.0.0.1"))   // 段越界
        assertFalse(DnsForm.isContentValid("A", "1.2.3"))       // 段数不足
        assertFalse(DnsForm.isContentValid("A", "1.2.3.4.5"))   // 段数过多
        assertFalse(DnsForm.isContentValid("A", "01.2.3.4"))    // 前导零
        assertFalse(DnsForm.isContentValid("A", "a.b.c.d"))     // 非数字
        assertFalse(DnsForm.isContentValid("A", ""))            // 空
        assertFalse(DnsForm.isContentValid("A", "2001:db8::1")) // IPv6 不是合法 A
    }

    @Test
    fun aaaaRecordValidatesIPv6() {
        assertTrue(DnsForm.isContentValid("AAAA", "2001:db8::1"))
        assertTrue(DnsForm.isContentValid("AAAA", "::1"))
        assertTrue(DnsForm.isContentValid("AAAA", "fe80::1234:5678:9abc:def0"))
        assertFalse(DnsForm.isContentValid("AAAA", "1.2.3.4"))      // IPv4 不是合法 AAAA
        assertFalse(DnsForm.isContentValid("AAAA", "xyz"))          // 无冒号
        assertFalse(DnsForm.isContentValid("AAAA", "::g::1"))       // 多个 "::" + 非法字符
    }

    @Test
    fun otherTypesOnlyRequireNonBlankContent() {
        assertTrue(DnsForm.isContentValid("CNAME", "example.com"))
        assertTrue(DnsForm.isContentValid("TXT", "v=spf1 -all"))
        assertTrue(DnsForm.isContentValid("MX", "mail.example.com"))
        assertTrue(DnsForm.isContentValid("NS", "ns1.example.com"))
        assertFalse(DnsForm.isContentValid("CNAME", "   "))
    }
}
