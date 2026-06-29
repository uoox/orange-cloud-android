package jiamin.chen.orangecloud

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.OAuthConfig
import jiamin.chen.orangecloud.core.design.theme.OrangeCloudTheme
import jiamin.chen.orangecloud.core.purchase.BillingGateway
import jiamin.chen.orangecloud.core.system.AppAppearance
import jiamin.chen.orangecloud.core.system.AppPrefs
import jiamin.chen.orangecloud.ui.root.OrangeCloudRoot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var billingGateway: BillingGateway

    @Inject
    lateinit var appPrefs: AppPrefs

    /** 热启动深链（QS 磁贴 / App Shortcuts 的 orangecloud open 深链）转交给 NavController。 */
    private val deepLinkIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingGateway.connect()
        handleOAuthRedirect(intent)
        setContent {
            val appearance by appPrefs.appearance.collectAsStateWithLifecycle(initialValue = AppAppearance.SYSTEM)
            val darkTheme = when (appearance) {
                AppAppearance.LIGHT -> false
                AppAppearance.DARK -> true
                AppAppearance.SYSTEM -> isSystemInDarkTheme()
            }
            OrangeCloudTheme(darkTheme = darkTheme) {
                OrangeCloudRoot(newIntents = deepLinkIntents)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // OAuth 回调自行处理；其余深链（冷启动由 NavHost 消费 intent，热启动经此 flow 转交）。
        if (!handleOAuthRedirect(intent) && intent.data != null) {
            deepLinkIntents.tryEmit(intent)
        }
    }

    /** 接住 Web 后端 302 跳回的 orangecloud://oauth/callback，交给 AuthRepository 验 state + 换 token。返回是否已消费。 */
    private fun handleOAuthRedirect(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        if (data.scheme != OAuthConfig.CALLBACK_SCHEME || data.host != OAuthConfig.CALLBACK_HOST) return false
        lifecycleScope.launch { authRepository.handleRedirect(data) }
        return true
    }
}
