package jiamin.chen.orangecloud.ui.workers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.WorkerMetrics
import jiamin.chen.orangecloud.data.model.WorkerScript
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.AnalyticsRepository
import jiamin.chen.orangecloud.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountStore: AccountStore,
    private val workerRepository: WorkerRepository,
    private val analyticsRepository: AnalyticsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val scriptName: String = checkNotNull(savedStateHandle["scriptName"])
    private val accountId: String? = accountStore.selectedAccountId.value

    val worker: StateFlow<WorkerScript?> =
        if (accountId == null) {
            MutableStateFlow(null)
        } else {
            workerRepository.observeWorkers(accountId)
                .map { list -> list.firstOrNull { it.id == scriptName } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    private val _metrics = MutableStateFlow<WorkerMetrics?>(null)
    val metrics: StateFlow<WorkerMetrics?> = _metrics.asStateFlow()

    init {
        // 详情依赖列表缓存；深链 / 进程恢复后缓存可能为空，主动刷新一次兜底（observeWorkers 会随之更新）。
        if (accountId != null) {
            viewModelScope.launch { runCatching { workerRepository.refreshWorkers(accountId) } }
        }
        if (accountId != null && authRepository.hasScope(Scopes.ANALYTICS_READ)) {
            viewModelScope.launch {
                _metrics.value = runCatching { analyticsRepository.workerMetrics(accountId, scriptName) }.getOrNull()
            }
        }
    }
}
