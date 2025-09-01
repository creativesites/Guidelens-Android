package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.craftflowtechnologies.guidelens.formatting.*
import com.craftflowtechnologies.guidelens.localization.*

/**
 * Demo component to showcase the EnhancedMessageRenderer
 */
@Composable
fun MessageRendererDemo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localizationManager = remember { LocalizationManager(context) }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Enhanced Message Renderer Demo",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        // Recipe Demo
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recipe Example",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EnhancedMessageRenderer(
                        formattedMessage = createRecipeDemo(),
                        localizationManager = localizationManager
                    )
                }
            }
        }
        
        // Craft Demo
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instructions Example",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EnhancedMessageRenderer(
                        formattedMessage = createInstructionsDemo(),
                        localizationManager = localizationManager,
                        onInstructionToggle = { index, completed ->
                            // Handle instruction toggle
                        }
                    )
                }
            }
        }
        
        // Text Demo
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Text Element Example",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EnhancedMessageRenderer(
                        formattedMessage = createTextDemo(),
                        localizationManager = localizationManager
                    )
                }
            }
        }
        
        // Localized Content Demo
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Localized Content Example",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EnhancedMessageRenderer(
                        formattedMessage = createLocalizedDemo(),
                        localizationManager = localizationManager
                    )
                }
            }
        }
    }
}

private fun createRecipeDemo(): FormattedMessage {
    return FormattedMessage(
        elements = listOf(
            MessageElement.Recipe(
                title = "Traditional Nshima with Relish",
                ingredients = listOf(
                    LocalizedIngredient(
                        name = "White cornmeal (mealie meal)",
                        localName = "Mealie meal",
                        alternatives = listOf("Corn flour", "Maize meal"),
                        availability = "Available at local shops"
                    ),
                    LocalizedIngredient(
                        name = "Pumpkin leaves",
                        localName = "Chibwabwa",
                        alternatives = listOf("Spinach", "Kale"),
                        availability = "Seasonal - best in rainy season"
                    )
                ),
                steps = listOf(
                    "Boil water in a heavy-bottomed pot",
                    "Gradually add mealie meal while stirring continuously",
                    "Cook for 15-20 minutes, stirring frequently",
                    "Prepare the relish with vegetables and protein"
                ),
                culturalNotes = "Nshima is the staple food of Zambia, traditionally eaten with hands and shared communally.",
                localizedTips = listOf(
                    "Use a wooden spoon (mwiko) for authentic stirring",
                    "The consistency should be thick enough to hold together when shaped"
                )
            )
        ),
        hasInteractiveContent = false,
        metadata = MessageMetadata(
            locale = com.craftflowtechnologies.guidelens.localization.LocaleInfo(
                countryCode = "ZM",
                countryName = "Zambia",
                culturalRegion = "Southern Province",
                primaryLanguage = "English",
                currency = "ZMW",
                timeZone = "Africa/Lusaka",
                climate = "Tropical",
                measurementSystem = "Metric",
                commonIngredients = listOf("maize meal", "vegetables"),
                seasons = "Hot, Rainy, Cool",
                culturalNotes = "Ubuntu philosophy",
                localChallenges = emptyList(),
                tribalLanguages = listOf("Tonga")
            ),
            tribalLanguage = "Tonga",
            agentType = "cooking"
        )
    )
}

private fun createInstructionsDemo(): FormattedMessage {
    return FormattedMessage(
        elements = listOf(
            MessageElement.Instructions(
                title = "Craft Project: Chitenge Bag",
                steps = listOf(
                    InstructionStep(1, "Choose your chitenge fabric pattern", false),
                    InstructionStep(2, "Cut fabric to required dimensions", false),
                    InstructionStep(3, "Create handles from fabric strips", false),
                    InstructionStep(4, "Sew sides and bottom together", true),
                    InstructionStep(5, "Attach handles securely", false),
                    InstructionStep(6, "Add finishing touches", false)
                ),
                allowInteraction = true
            )
        ),
        hasInteractiveContent = true,
        metadata = MessageMetadata(
            locale = com.craftflowtechnologies.guidelens.localization.LocaleInfo(
                countryCode = "ZM",
                countryName = "Zambia",
                culturalRegion = "Eastern Province",
                primaryLanguage = "English",
                currency = "ZMW",
                timeZone = "Africa/Lusaka",
                climate = "Tropical",
                measurementSystem = "Metric",
                commonIngredients = listOf("maize meal", "vegetables"),
                seasons = "Hot, Rainy, Cool",
                culturalNotes = "Ubuntu philosophy",
                localChallenges = emptyList(),
                tribalLanguages = listOf("Nyanja")
            ),
            tribalLanguage = "Nyanja",
            agentType = "crafting"
        )
    )
}

private fun createTextDemo(): FormattedMessage {
    return FormattedMessage(
        elements = listOf(
            MessageElement.Text(
                content = "Welcome to GuideLens",
                style = TextStyle.HEADING
            ),
            MessageElement.Text(
                content = "Your AI-powered assistant for cooking, crafting, and DIY projects. Get personalized guidance adapted to your local context and cultural preferences.",
                style = TextStyle.BODY
            ),
            MessageElement.Text(
                content = "✨ Localized for Zambian culture",
                style = TextStyle.EMPHASIS
            )
        ),
        hasInteractiveContent = false,
        metadata = MessageMetadata(
            locale = com.craftflowtechnologies.guidelens.localization.LocaleInfo(
                countryCode = "ZM",
                countryName = "Zambia",
                culturalRegion = "Central Province",
                primaryLanguage = "English",
                currency = "ZMW",
                timeZone = "Africa/Lusaka",
                climate = "Tropical",
                measurementSystem = "Metric",
                commonIngredients = listOf("maize meal", "vegetables"),
                seasons = "Hot, Rainy, Cool",
                culturalNotes = "Ubuntu philosophy",
                localChallenges = emptyList(),
                tribalLanguages = listOf("Bemba")
            ),
            tribalLanguage = "Bemba",
            agentType = "buddy"
        )
    )
}

private fun createLocalizedDemo(): FormattedMessage {
    return FormattedMessage(
        elements = listOf(
            MessageElement.LocalizedContent(
                content = "Mwabuka buti! Let me help you prepare a delicious meal using ingredients commonly found in Zambian markets.",
                culturalExpressions = listOf(
                    CulturalExpression(
                        phrase = "Mwabuka buti",
                        meaning = "How did you wake up?",
                        context = "Common morning greeting in Bemba"
                    )
                ),
                localizedTerms = listOf(
                    LocalizedTerm(
                        phrase = "Salaula",
                        translation = "Second-hand clothes market",
                        language = "Bemba"
                    )
                ),
                seasonalContext = "During the rainy season (November-April), fresh vegetables are abundant in local markets."
            )
        ),
        hasInteractiveContent = false,
        metadata = MessageMetadata(
            locale = com.craftflowtechnologies.guidelens.localization.LocaleInfo(
                countryCode = "ZM",
                countryName = "Zambia",
                culturalRegion = "Copperbelt",
                primaryLanguage = "English",
                currency = "ZMW",
                timeZone = "Africa/Lusaka",
                climate = "Tropical",
                measurementSystem = "Metric",
                commonIngredients = listOf("maize meal", "vegetables"),
                seasons = "Hot, Rainy, Cool",
                culturalNotes = "Ubuntu philosophy",
                localChallenges = emptyList(),
                tribalLanguages = listOf("Bemba")
            ),
            tribalLanguage = "Bemba",
            agentType = "buddy"
        )
    )
}