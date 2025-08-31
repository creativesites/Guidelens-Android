package com.craftflowtechnologies.guidelens.prompts

import com.craftflowtechnologies.guidelens.localization.LocalizationManager
import com.craftflowtechnologies.guidelens.personalization.AgentType

/**
 * Specialized prompts for generating chart-ready data in AI responses
 * These prompts guide the AI to format data in ways that can be parsed into charts
 */
class ChartDataPrompts(private val localizationManager: LocalizationManager) {
    
    /**
     * Generate comprehensive prompt instructions for chart data generation
     */
    suspend fun generateChartDataPrompt(
        agentType: AgentType,
        userQuery: String,
        includeCharts: Boolean = true
    ): String {
        val localizationContext = localizationManager.generateLocalizedPromptContext()
        
        return buildString {
            // Base instruction for structured data
            appendLine("STRUCTURED DATA FORMATTING INSTRUCTIONS:")
            appendLine("When providing responses that contain numerical data, measurements, progress tracking, or breakdowns, format them in a way that enables visualization.")
            appendLine()
            
            if (includeCharts) {
                appendLine("CHART DATA FORMATS:")
                appendLine()
                
                // Add agent-specific chart instructions
                when (agentType) {
                    AgentType.COOKING -> appendCookingChartInstructions()
                    AgentType.CRAFTING -> appendCraftingChartInstructions()
                    AgentType.DIY -> appendDIYChartInstructions()
                    AgentType.BUDDY -> appendBuddyChartInstructions()
                }
                
                appendLine()
                appendLine("GENERAL CHART FORMATTING RULES:")
                appendGeneralChartRules()
            }
            
            appendLine()
            appendLine("LOCALIZATION REQUIREMENTS:")
            append(localizationContext)
            
            appendLine()
            appendLine("ENHANCED MESSAGE FORMATTING:")
            appendMessageFormattingRules()
            
            appendLine()
            appendLine("USER QUERY:")
            appendLine(userQuery)
            
            appendLine()
            appendLine("RESPONSE REQUIREMENTS:")
            appendLine("- Provide helpful, accurate information")
            appendLine("- Use structured formatting for any numerical data")
            appendLine("- Include cultural context where appropriate")
            appendLine("- Format costs in local currency")
            appendLine("- Consider local availability of ingredients/materials")
            appendLine("- Add seasonal awareness to suggestions")
        }
    }
    
    private fun StringBuilder.appendCookingChartInstructions() {
        appendLine("NUTRITION DATA FORMAT:")
        appendLine("When discussing recipes or food, include nutritional information like this:")
        appendLine("Nutrition per serving:")
        appendLine("Calories: 350")
        appendLine("Proteins: 25g")
        appendLine("Carbohydrates: 45g")
        appendLine("Fats: 12g")
        appendLine("Fiber: 8g")
        appendLine("Sugar: 5g")
        appendLine("Sodium: 650mg")
        appendLine()
        
        appendLine("RECIPE FORMAT:")
        appendLine("Recipe: [Name]")
        appendLine("Ingredients:")
        appendLine("- [ingredient 1]")
        appendLine("- [ingredient 2]")
        appendLine("Instructions:")
        appendLine("1. [step 1]")
        appendLine("2. [step 2]")
        appendLine()
        
        appendLine("COOKING PROGRESS FORMAT:")
        appendLine("Cooking Skills Progress:")
        appendLine("Knife Skills: 7/10")
        appendLine("Seasoning: 8/10") 
        appendLine("Timing: 6/10")
        appendLine("Plating: 5/10")
        appendLine()
        
        appendLine("MEAL COST FORMAT:")
        appendLine("Cost Breakdown:")
        appendLine("Ingredients: K25.50")
        appendLine("Spices: K5.00")
        appendLine("Gas/Electricity: K3.50")
        appendLine("Total: K34.00")
        appendLine()
    }
    
    private fun StringBuilder.appendCraftingChartInstructions() {
        appendLine("CRAFTING PROGRESS FORMAT:")
        appendLine("Project Progress:")
        appendLine("Design: 8/10")
        appendLine("Material Prep: 6/10")
        appendLine("Assembly: 4/10")
        appendLine("Finishing: 2/10")
        appendLine()
        
        appendLine("CRAFTING COST FORMAT:")
        appendLine("Project Cost:")
        appendLine("Materials: K15.00")
        appendLine("Tools: K8.00")
        appendLine("Decorations: K5.50")
        appendLine("Total: K28.50")
        appendLine()
        
        appendLine("SKILL DEVELOPMENT FORMAT:")
        appendLine("Crafting Skills:")
        appendLine("Basic Techniques: 9/10")
        appendLine("Color Theory: 6/10")
        appendLine("Pattern Design: 7/10")
        appendLine("Problem Solving: 8/10")
        appendLine()
    }
    
    private fun StringBuilder.appendDIYChartInstructions() {
        appendLine("DIY PROJECT COST FORMAT:")
        appendLine("Project Cost Estimate:")
        appendLine("Materials: K120.00")
        appendLine("Tools: K45.00")
        appendLine("Hardware: K25.00")
        appendLine("Permits: K15.00")
        appendLine("Contingency: K30.00")
        appendLine("Total: K235.00")
        appendLine()
        
        appendLine("DIY PROGRESS FORMAT:")
        appendLine("Project Progress:")
        appendLine("Planning: 10/10")
        appendLine("Material Gathering: 8/10")
        appendLine("Construction: 5/10")
        appendLine("Finishing: 0/10")
        appendLine()
        
        appendLine("SAFETY CHECKLIST FORMAT:")
        appendLine("Safety Requirements:")
        appendLine("- Personal Protective Equipment")
        appendLine("- Tool inspection completed") 
        appendLine("- Work area prepared")
        appendLine("- Emergency contacts available")
        appendLine()
    }
    
    private fun StringBuilder.appendBuddyChartInstructions() {
        appendLine("MOOD TRACKING FORMAT:")
        appendLine("Recent Mood Data:")
        appendLine("12/15: 4.0")
        appendLine("12/16: 3.5")  
        appendLine("12/17: 4.5")
        appendLine("12/18: 3.0")
        appendLine("12/19: 4.2")
        appendLine("(Scale: 1-5 where 1=very sad, 5=very happy)")
        appendLine()
        
        appendLine("WELLNESS PROGRESS FORMAT:")
        appendLine("Wellness Goals Progress:")
        appendLine("Daily Exercise: 6/7 days")
        appendLine("Meditation: 4/7 days")
        appendLine("Sleep Quality: 7/10")
        appendLine("Stress Management: 8/10")
        appendLine()
        
        appendLine("ACTIVITY BREAKDOWN FORMAT:")
        appendLine("This Week's Activities:")
        appendLine("Self-care: 3 hours")
        appendLine("Social Connection: 5 hours")
        appendLine("Learning: 4 hours") 
        appendLine("Recreation: 6 hours")
        appendLine()
    }
    
    private fun StringBuilder.appendGeneralChartRules() {
        appendLine("1. NUMERICAL DATA: Always provide specific numbers, not ranges")
        appendLine("2. CURRENCY: Use local currency symbols (K for Kwacha, $ for USD)")
        appendLine("3. MEASUREMENTS: Use metric system primarily")
        appendLine("4. PROGRESS: Express as current/total format (e.g., 7/10)")
        appendLine("5. PERCENTAGES: Include both fraction and percentage when helpful")
        appendLine("6. TIME PERIODS: Specify timeframes for data (daily, weekly, monthly)")
        appendLine("7. CATEGORIES: Group related items together")
        appendLine("8. TOTALS: Always provide totals for cost breakdowns")
        appendLine("9. UNITS: Include units for all measurements")
        appendLine("10. CONTEXT: Explain what the numbers represent")
    }
    
    private fun StringBuilder.appendMessageFormattingRules() {
        appendLine("MESSAGE STRUCTURE:")
        appendLine("- Use clear headings for different sections")
        appendLine("- Separate instructions into numbered steps")
        appendLine("- Group related information together")
        appendLine("- Include cultural context where relevant")
        appendLine("- Provide local alternatives and availability")
        appendLine("- Add practical tips for local conditions")
        appendLine()
        
        appendLine("INSTRUCTION FORMATTING:")
        appendLine("When providing instructions, format as:")
        appendLine("Instructions:")
        appendLine("1. First step with clear action")
        appendLine("2. Second step with specific details")
        appendLine("3. Third step with expected outcome")
        appendLine()
        
        appendLine("LOCALIZED CONTENT:")
        appendLine("- Reference local ingredients, materials, or tools when possible")
        appendLine("- Include local names in parentheses")
        appendLine("- Consider local climate and seasonal factors")
        appendLine("- Address common local challenges")
        appendLine("- Use appropriate cultural expressions and wisdom")
    }
    
    /**
     * Generate prompts for specific chart types
     */
    fun getNutritionAnalysisPrompt(): String {
        return """
            Please analyze the nutritional content of this recipe/meal and provide detailed breakdown.
            Format the response to include:
            
            Nutrition per serving:
            Calories: [number]
            Proteins: [number]g
            Carbohydrates: [number]g  
            Fats: [number]g
            Fiber: [number]g
            Sugar: [number]g
            Sodium: [number]mg
            
            Also provide health insights and dietary considerations relevant to the local context.
        """.trimIndent()
    }
    
    fun getMoodTrackingPrompt(): String {
        return """
            Help track mood and emotional wellbeing. Format mood data as:
            
            Recent Mood Data:
            [date]: [1-5 rating]
            [date]: [1-5 rating]
            
            Scale: 1=very sad, 2=sad, 3=neutral, 4=good, 5=very happy
            
            Provide supportive insights and suggestions for emotional wellness.
        """.trimIndent()
    }
    
    fun getProgressTrackingPrompt(): String {
        return """
            Track skill development and project progress. Format as:
            
            [Category] Progress:
            [Skill/Task]: [current]/[total]
            [Skill/Task]: [current]/[total]
            
            Provide encouragement and specific next steps for improvement.
        """.trimIndent()
    }
    
    fun getCostEstimationPrompt(): String {
        val currency = localizationManager.currentLocale.value.currency
        val currencySymbol = when (currency) {
            "ZMW" -> "K"
            "USD" -> "$"
            else -> currency
        }
        
        return """
            Calculate and break down costs in local currency ($currency).
            Format as:
            
            Cost Breakdown:
            [Item Category]: $currencySymbol[amount]
            [Item Category]: $currencySymbol[amount]
            Total: $currencySymbol[total]
            
            Consider local pricing, availability, and economic conditions.
            Include money-saving tips relevant to the local context.
        """.trimIndent()
    }
    
    /**
     * Generate example responses for training/consistency
     */
    fun getExampleChartResponses(): Map<String, String> {
        return mapOf(
            "nutrition_example" to """
                This nshima with chicken recipe provides excellent nutrition for the whole family!
                
                Nutrition per serving:
                Calories: 420
                Proteins: 28g
                Carbohydrates: 52g
                Fats: 8g
                Fiber: 6g
                Sugar: 3g
                Sodium: 680mg
                
                This balanced meal provides sustained energy and is rich in protein from the chicken. In our Zambian tradition, sharing this meal strengthens family bonds and ensures everyone gets proper nutrition.
                
                Local tip: Add more vegetables like rape or pumpkin leaves to increase fiber and vitamins. During the hot season, drink plenty of water with your meals.
            """.trimIndent(),
            
            "cost_example" to """
                Here's the cost breakdown for your DIY kitchen cabinet project:
                
                Cost Breakdown:
                Materials: K180.00
                Hardware: K45.00
                Tools: K25.00
                Finishing: K30.00
                Contingency: K40.00
                Total: K320.00
                
                Money-saving tips for Zambia:
                - Buy timber directly from the sawmill to save 20-30%
                - Shop at Kamwala or similar markets for hardware
                - Consider buying second-hand tools in good condition
                - Plan your project during the dry season for easier work
                
                Remember to factor in transport costs if materials need delivery.
            """.trimIndent(),
            
            "mood_example" to """
                I can see you're tracking your emotional journey - that's wonderful self-awareness!
                
                Recent Mood Data:
                12/15: 3.5
                12/16: 4.0
                12/17: 3.0
                12/18: 4.2
                12/19: 4.5
                
                Your mood has been generally positive with some natural ups and downs. The upward trend at the end of the week is encouraging!
                
                As we say in Bemba, "Umwana ashenda atasha nyina ukwabula" - through your journey of self-reflection, you're gaining wisdom. Keep nurturing your emotional health with small daily practices.
                
                Would you like to explore what contributed to your better days?
            """.trimIndent()
        )
    }
}