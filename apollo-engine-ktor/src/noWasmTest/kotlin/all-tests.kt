@file:Suppress("DEPRECATION")
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.ktor.http.KtorHttpEngine
import com.apollographql.ktor.ws.KtorWebSocketEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class AllTests {
    @OptIn(ExperimentalCoroutinesApi::class, ApolloInternal::class)
    @Test
    fun runAllTests() = runTest {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            com.apollographql.apollo.engine.tests.runAllTests(
                engine = { KtorHttpEngine(it) },
                webSocketEngine = { KtorWebSocketEngine() },
                false
            )
        }
    }
}
