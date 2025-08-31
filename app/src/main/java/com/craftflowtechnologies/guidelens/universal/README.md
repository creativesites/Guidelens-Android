# Universal Artifacts Overlay System

This package contains the universal artifacts overlay system that replaces the cooking-specific `InteractiveCookingOverlay` with a generic, agent-aware overlay that works across all GuideLens skill domains.

## Architecture Overview

### Core Components

1. **UniversalArtifactSessionManager** - Generic session management for all artifact types
2. **UniversalArtifactOverlay** - Main overlay component with agent-aware UI  
3. **ContentAdapters** - Type-specific adapters for different content types
4. **UniversalAgentIntegration** - AI routing layer for different agents

### Key Benefits

- **Code Reusability**: 80% shared overlay logic across all agents
- **Consistent UX**: Uniform experience across cooking, crafting, DIY, and learning
- **Easier Maintenance**: Single overlay component to maintain instead of four
- **Extensible**: Easy to add new agent types and content adapters
- **Type Safety**: Full Kotlin type safety with sealed classes and generics

## File Structure

```
universal/
├── UniversalSessionManager.kt      # Core session management
├── ContentAdapters.kt              # Type-specific adapters  
├── UniversalArtifactOverlay.kt     # Main overlay component
├── UniversalOverlayComponents.kt   # Shared UI components
├── UniversalStepComponents.kt      # Step-related components
├── UniversalUIComponents.kt        # Reusable UI elements
├── UniversalAgentIntegration.kt    # AI agent integration
├── UniversalOverlayMigration.kt    # Migration examples
└── README.md                       # This file
```

## Usage

### Basic Usage

```kotlin
@Composable
fun MyScreen() {
    val artifact = createRecipeArtifact() // Any artifact type
    
    UniversalArtifactOverlay(
        artifact = artifact,
        sessionManager = universalSessionManager,
        imageGenerator = imageGenerator,
        onSendMessage = { message -> /* Handle AI message */ },
        onRequestImage = { prompt, step -> /* Generate image */ },
        onCaptureProgress = { /* Capture progress photo */ },
        onDismiss = { /* Dismiss overlay */ },
        themeController = themeController
    )
}
```

### Agent-Specific Features

The overlay automatically adapts based on `artifact.agentType`:

- **Cooking**: Temperature displays, timer controls, ingredient tracking
- **Crafting**: Material lists, color palettes, technique guides  
- **DIY**: Safety warnings, tool requirements, measurement helpers
- **Buddy**: Progress tracking, exercises, knowledge checks

### Content Adapters

Each artifact type has a dedicated adapter:

```kotlin
val adapter = ContentAdapterFactory.createAdapter(artifact)

// Get agent-specific data
val stepTitle = adapter.getCurrentStepTitle(artifact, stepIndex)
val techniques = adapter.getStepTechniques(artifact, stepIndex)
val duration = adapter.getStepDuration(artifact, stepIndex)
```

### Session Management

The universal session manager handles all artifact types:

```kotlin
// Start session for any artifact type
val result = sessionManager.startSession(
    userId = "user123",
    artifact = artifact,
    environmentalContext = mapOf("location" to "kitchen")
)

// Navigate steps universally
sessionManager.nextStep()
sessionManager.previousStep()
sessionManager.completeStep("step_1")

// Pause/resume sessions
sessionManager.pauseSession()
sessionManager.resumeSession()
```

## Agent Integration

### AI Routing

The system routes AI requests to appropriate agents:

```kotlin
val agentIntegration = UniversalAgentIntegration(geminiClient, progressSystem)

// Generate agent-specific responses
val response = agentIntegration.generateResponse(
    message = "I need help with this step",
    session = currentSession,
    contentAdapter = adapter
)
```

### Agent Prompts

Each agent has specialized prompts:

- **Cooking Agent**: Focus on technique, safety, timing, substitutions
- **Crafting Guru**: Emphasize creativity, patience, technique improvement  
- **DIY Helper**: Prioritize safety, proper tools, step-by-step guidance
- **Buddy**: Supportive learning, skill assessment, encouragement

## Migration Guide

### From InteractiveCookingOverlay

Replace the old cooking-specific overlay:

```kotlin
// Old way
InteractiveCookingOverlay(
    recipe = recipe,
    cookingSessionManager = cookingSessionManager,
    // ... other cooking-specific params
)

// New way
UniversalArtifactOverlay(
    artifact = recipeArtifact, // Convert recipe to artifact
    sessionManager = universalSessionManager, 
    // ... universal params
)
```

### Key Differences

1. **Session Data**: `EnhancedCookingSession` → `UniversalArtifactSession`
2. **Context**: `SessionContext` → `UniversalSessionContext`  
3. **Messages**: `ChatMessage` → `UniversalChatMessage`
4. **Progress**: Cooking-specific → Universal with adapters

## Supported Artifact Types

| Type | Agent | Content Adapter | Features |
|------|--------|----------------|----------|
| RECIPE | cooking | RecipeContentAdapter | Timers, temperatures, ingredients |
| CRAFT_PROJECT | crafting | CraftingContentAdapter | Materials, tools, techniques |
| DIY_GUIDE | diy | DIYContentAdapter | Safety warnings, measurements |
| LEARNING_MODULE | buddy | TutorialContentAdapter | Exercises, objectives |
| SKILL_TUTORIAL | buddy | TutorialContentAdapter | Progress tracking, assessments |

## Theming and Styling

### Agent-Specific Colors

```kotlin
object UniversalAgentTheme {
    val Cooking = AgentColors(
        primary = Color(0xFF32D74B),    // Green
        secondary = Color(0xFFFF9500),  // Orange
        accent = Color(0xFF007AFF)      // Blue
    )
    
    val Crafting = AgentColors(
        primary = Color(0xFFBF5AF2),    // Purple  
        secondary = Color(0xFF00C7BE),  // Teal
        accent = Color(0xFF30D158)      // Green
    )
    // ... other agents
}
```

### Dynamic Theming

The overlay automatically adapts colors, icons, and messaging based on the agent type.

## Advanced Features

### Preview Mode

Users can preview steps without affecting session progress:

```kotlin
// Toggle preview mode
onTogglePreviewMode(true)

// Navigation in preview doesn't affect actual progress
onNextStep() // Only advances preview, not session
```

### Progress Snapshots

Capture progress at any step:

```kotlin
val snapshot = UniversalProgressSnapshot(
    timestamp = System.currentTimeMillis(),
    stageIndex = currentStep,
    imageUrl = capturedImageUrl,
    aiAnalysis = analysisResult,
    userNote = userNote,
    agentType = session.agentType
)
```

### Emotional State Tracking

The system tracks user emotional state for better responses:

```kotlin
enum class EmotionalState {
    CONFIDENT, NEUTRAL, CONFUSED, 
    FRUSTRATED, EXCITED, TIRED, 
    FOCUSED, OVERWHELMED
}
```

## Performance Considerations

### Lazy Loading

Components are lazily loaded based on agent type:

```kotlin
@Composable
fun AgentSpecificComponent(agentType: String) {
    when (agentType) {
        "cooking" -> LazyLoadCookingFeatures()
        "crafting" -> LazyLoadCraftingFeatures()
        // ... other agents
    }
}
```

### Memory Management

- Session data is automatically cleaned up
- Image references are weakly held
- Large datasets are paginated

## Testing

### Unit Tests

```kotlin
@Test
fun `content adapter returns correct step data`() {
    val adapter = RecipeContentAdapter()
    val stepData = UniversalStepData.fromAdapter(adapter, artifact, 0)
    
    assertEquals("Mix dry ingredients", stepData.title)
    assertTrue(stepData.techniques.contains("mixing"))
}
```

### Integration Tests

```kotlin
@Test
fun `universal session manager handles all artifact types`() {
    val artifacts = listOf(
        createRecipeArtifact(),
        createCraftingArtifact(), 
        createDIYArtifact(),
        createLearningArtifact()
    )
    
    artifacts.forEach { artifact ->
        val result = sessionManager.startSession("user", artifact)
        assertTrue(result.isSuccess)
    }
}
```

## Future Enhancements

1. **Voice Integration**: Universal voice commands across all agents
2. **AR Support**: Augmented reality overlays for visual tasks
3. **Collaborative Sessions**: Multi-user sessions with role-based permissions
4. **Advanced Analytics**: Cross-agent learning pattern analysis
5. **Plugin System**: Third-party agent extensions

## Contributing

When adding new agent types or content adapters:

1. Implement the `ContentAdapter` interface
2. Add agent colors to `UniversalAgentTheme`
3. Update `ContentAdapterFactory`
4. Add system prompts to `UniversalAgentIntegration`
5. Create example artifacts in `UniversalArtifactExamples`
6. Add comprehensive tests

## Troubleshooting

### Common Issues

**Q: Overlay doesn't show agent-specific features**
A: Ensure `artifact.agentType` matches the content adapter type

**Q: Session state not persisting**
A: Check that `UniversalArtifactSessionManager` is properly retained

**Q: Wrong colors/theming**
A: Verify `ContentAdapter` returns correct colors for the agent type

**Q: AI responses are generic**  
A: Check `UniversalAgentIntegration` prompt routing for the agent type

### Debug Logging

Enable debug logging:

```kotlin
Log.d("UniversalOverlay", "Current agent: ${session.agentType}")
Log.d("UniversalOverlay", "Step data: ${stepData}")
```

## License

Part of the GuideLens project. See project license for details.