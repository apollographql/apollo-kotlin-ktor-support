# Welcome

Add the dependency:

```kotlin title="build.gradle[.kts]"
dependencies {
  // ...
  implementation("com.apollographql.ktor:apollo-engine-ktor:%latest_version%")
}
```

And configure your `ApolloClient` to use the Ktor engine:

```kotlin
val client = ApolloClient.Builder()
  .serverUrl("https://example.com/graphql")
  // Create a new Ktor engine
  .httpEngine(KtorHttpEngine())
  // Or, if you want to reuse an existing Ktor client
  .ktorClient(myKtorClient)
  .build()
```