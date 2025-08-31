package com.craftflowtechnologies.guidelens.companion

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import androidx.compose.ui.graphics.Color
import com.craftflowtechnologies.guidelens.storage.*
import java.text.SimpleDateFormat
import java.util.*

class BuddyToolsManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _moodHistory = MutableStateFlow<List<MoodEntry>>(emptyList())
    val moodHistory: StateFlow<List<MoodEntry>> = _moodHistory.asStateFlow()
    
    private val _wellnessGoals = MutableStateFlow<List<WellnessGoal>>(emptyList())
    val wellnessGoals: StateFlow<List<WellnessGoal>> = _wellnessGoals.asStateFlow()
    
    private val _supportSessions = MutableStateFlow<List<SupportSession>>(emptyList())
    val supportSessions: StateFlow<List<SupportSession>> = _supportSessions.asStateFlow()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    companion object {
        private const val TAG = "BuddyToolsManager"
        
        // Learning artifact templates
        private val LEARNING_TEMPLATES = mapOf(
            "personal_development" to "Personal Development Plan",
            "wellness_program" to "Wellness & Mental Health Program", 
            "skill_building" to "Skill Building Journey",
            "habit_formation" to "Habit Formation Guide",
            "mindfulness_practice" to "Mindfulness Practice Program",
            "confidence_building" to "Confidence Building Workshop",
            "stress_management" to "Stress Management Toolkit",
            "goal_achievement" to "Goal Achievement System"
        )
    }

    fun getBuddyTools(): List<BuddyTool> {
        return listOf(
            BuddyTool(
                id = "mood_tracker",
                name = "Mood Check-In",
                description = "How are you feeling today? Let's track your emotional journey",
                icon = Icons.Default.Mood,
                action = { trackMood() },
                category = BuddyCategory.EMOTIONAL_SUPPORT,
                duration = "3-5 minutes",
                benefits = listOf("Self-awareness", "Pattern recognition", "Emotional intelligence")
            ),
            BuddyTool(
                id = "breathing_exercise",
                name = "Calm Together",
                description = "Let's practice some calming breathing exercises together",
                icon = Icons.Default.Air,
                action = { guidedBreathing() },
                category = BuddyCategory.RELAXATION,
                duration = "5-15 minutes",
                benefits = listOf("Stress relief", "Better focus", "Calm energy")
            ),
            BuddyTool(
                id = "gratitude_practice",
                name = "Gratitude Moments",
                description = "Share the good things in your life - let's celebrate together",
                icon = Icons.Default.FavoriteBorder,
                action = { practiceGratitude() },
                category = BuddyCategory.POSITIVITY,
                duration = "5-10 minutes",
                benefits = listOf("Positive mindset", "Appreciation", "Joy")
            ),
            BuddyTool(
                id = "energy_booster",
                name = "Energy Boost",
                description = "Feeling low? Let's find ways to lift your spirits",
                icon = Icons.Default.Bolt,
                action = { boostEnergy() },
                category = BuddyCategory.MOTIVATION,
                duration = "3-10 minutes",
                benefits = listOf("Increased energy", "Motivation", "Positive vibes")
            ),
            BuddyTool(
                id = "mindfulness_moment",
                name = "Present Moment",
                description = "Let's be mindful and present together, right here, right now",
                icon = Icons.Default.SelfImprovement,
                action = { mindfulnessMoment() },
                category = BuddyCategory.MINDFULNESS,
                duration = "5-20 minutes",
                benefits = listOf("Present awareness", "Peace", "Clarity")
            ),
            BuddyTool(
                id = "goal_setting",
                name = "Dream Builder",
                description = "Let's talk about your goals and dreams - I'll help you plan",
                icon = Icons.Default.Flag,
                action = { setGoals() },
                category = BuddyCategory.GROWTH,
                duration = "10-20 minutes",
                benefits = listOf("Direction", "Motivation", "Achievement")
            ),
            BuddyTool(
                id = "self_care_reminder",
                name = "Self-Care Check",
                description = "Have you been taking care of yourself? Let's make a plan",
                icon = Icons.Default.Spa,
                action = { selfCareCheck() },
                category = BuddyCategory.SELF_CARE,
                duration = "8-15 minutes",
                benefits = listOf("Better health", "Self-love", "Balance")
            ),
            BuddyTool(
                id = "stress_relief",
                name = "Stress Buster",
                description = "Feeling overwhelmed? Let's work through it together",
                icon = Icons.Default.Healing,
                action = { relieveStress() },
                category = BuddyCategory.STRESS_MANAGEMENT,
                duration = "10-25 minutes",
                benefits = listOf("Stress reduction", "Clarity", "Calm")
            ),
            BuddyTool(
                id = "confidence_builder",
                name = "Confidence Boost",
                description = "Let's remind you of how amazing you are",
                icon = Icons.Default.Stars,
                action = { buildConfidence() },
                category = BuddyCategory.CONFIDENCE,
                duration = "8-15 minutes",
                benefits = listOf("Self-esteem", "Confidence", "Self-worth")
            ),
            BuddyTool(
                id = "sleep_support",
                name = "Sleep Well",
                description = "Having trouble sleeping? Let's create a peaceful bedtime routine",
                icon = Icons.Default.Bedtime,
                action = { supportSleep() },
                category = BuddyCategory.SLEEP,
                duration = "10-30 minutes",
                benefits = listOf("Better sleep", "Relaxation", "Rest")
            ),
            BuddyTool(
                id = "friendship_chat",
                name = "Friendly Chat",
                description = "Sometimes you just need someone to talk to - I'm here to listen",
                icon = Icons.Default.Chat,
                action = { friendlyChat() },
                category = BuddyCategory.COMPANIONSHIP,
                duration = "10-45 minutes",
                benefits = listOf("Connection", "Support", "Understanding")
            ),
            BuddyTool(
                id = "celebration_helper",
                name = "Celebration Time",
                description = "Did something good happen? Let's celebrate your wins together!",
                icon = Icons.Default.Celebration,
                action = { celebrate() },
                category = BuddyCategory.CELEBRATION,
                duration = "5-15 minutes",
                benefits = listOf("Joy", "Recognition", "Positive reinforcement")
            ),
            
            // Artifact-generating tools for structured learning programs
            BuddyTool(
                id = "personal_development_plan",
                name = "Personal Growth Plan",
                description = "Create a comprehensive personal development journey tailored to your goals",
                icon = Icons.Default.TrendingUp,
                action = { createPersonalDevelopmentPlan() },
                category = BuddyCategory.GROWTH,
                duration = "20-30 minutes",
                benefits = listOf("Clear direction", "Structured growth", "Achievement tracking"),
                suitableFor = listOf("Goal-oriented individuals", "Career development", "Self-improvement seekers")
            ),
            
            BuddyTool(
                id = "wellness_program",
                name = "Wellness Program",
                description = "Design a holistic mental health and wellness program for sustainable well-being",
                icon = Icons.Default.Psychology,
                action = { BuddyActionResult.Error("This feature is coming soon!") },
                category = BuddyCategory.SELF_CARE,
                duration = "25-35 minutes", 
                benefits = listOf("Balanced lifestyle", "Mental clarity", "Emotional resilience"),
                suitableFor = listOf("Stress management", "Mental health focus", "Holistic wellness")
            ),
            
            BuddyTool(
                id = "skill_building_journey",
                name = "Skill Mastery Path",
                description = "Create a structured learning journey for developing any new skill",
                icon = Icons.Default.School,
                action = { BuddyActionResult.Error("This feature is coming soon!") },
                category = BuddyCategory.GROWTH,
                duration = "15-25 minutes",
                benefits = listOf("Skill acquisition", "Learning structure", "Progress tracking"),
                suitableFor = listOf("Learning new skills", "Career advancement", "Hobby development")
            ),
            
            BuddyTool(
                id = "habit_formation_guide",
                name = "Habit Builder",
                description = "Design a science-based habit formation program that actually sticks",
                icon = Icons.Default.Repeat,
                action = { BuddyActionResult.Error("This feature is coming soon!") },
                category = BuddyCategory.GROWTH,
                duration = "18-25 minutes",
                benefits = listOf("Lasting change", "Automatic behaviors", "Compound growth"),
                suitableFor = listOf("Building good habits", "Breaking bad habits", "Lifestyle change")
            ),
            
            BuddyTool(
                id = "mindfulness_program",
                name = "Mindfulness Journey",
                description = "Create a personalized mindfulness and meditation practice program",
                icon = Icons.Default.SelfImprovement,
                action = { BuddyActionResult.Error("This feature is coming soon!") },
                category = BuddyCategory.MINDFULNESS,
                duration = "20-30 minutes",
                benefits = listOf("Present awareness", "Emotional regulation", "Mental clarity"),
                suitableFor = listOf("Stress reduction", "Meditation beginners", "Spiritual growth")
            ),
            
            BuddyTool(
                id = "confidence_workshop",
                name = "Confidence Workshop",
                description = "Build a comprehensive confidence-building program with actionable steps",
                icon = Icons.Default.Star,
                action = { BuddyActionResult.Error("This feature is coming soon!") },
                category = BuddyCategory.CONFIDENCE,
                duration = "22-30 minutes",
                benefits = listOf("Self-assurance", "Improved self-image", "Social confidence"),
                suitableFor = listOf("Low self-esteem", "Social anxiety", "Career confidence")
            )
        )
    }

    private suspend fun trackMood(): BuddyActionResult {
        val moodScale = listOf(
            MoodOption("😢", "Very Sad", 1, Color(0xFF8B5CF6)),
            MoodOption("😔", "Sad", 2, Color(0xFF3B82F6)),
            MoodOption("😐", "Neutral", 3, Color(0xFF6B7280)),
            MoodOption("🙂", "Good", 4, Color(0xFF10B981)),
            MoodOption("😄", "Very Happy", 5, Color(0xFFF59E0B))
        )
        
        val followUpQuestions = listOf(
            "What's contributing to how you're feeling right now?",
            "Is there anything specific that happened today?",
            "What would help you feel even better?",
            "What are you grateful for in this moment?"
        )
        
        return BuddyActionResult.Success(
            title = "How Are You Feeling?",
            message = "I'm so glad you're checking in with yourself. Your feelings are valid and important.",
            moodOptions = moodScale,
            followUpQuestions = followUpQuestions,
            encouragement = listOf(
                "It's okay to have difficult days - you're not alone",
                "Every feeling is temporary and serves a purpose",
                "You're being so brave by acknowledging how you feel",
                "I'm here with you, no matter what you're going through"
            )
        )
    }

    private suspend fun guidedBreathing(): BuddyActionResult {
        val breathingTechniques = listOf(
            BreathingTechnique(
                name = "Box Breathing",
                description = "Breathe in for 4, hold for 4, out for 4, hold for 4",
                duration = "5-10 minutes",
                benefits = listOf("Reduces anxiety", "Improves focus", "Calms nervous system")
            ),
            BreathingTechnique(
                name = "4-7-8 Technique",
                description = "Breathe in for 4, hold for 7, out for 8",
                duration = "3-5 minutes",
                benefits = listOf("Promotes relaxation", "Helps with sleep", "Reduces stress")
            ),
            BreathingTechnique(
                name = "Simple Deep Breathing",
                description = "Just breathe slowly and deeply with me",
                duration = "2-15 minutes",
                benefits = listOf("Immediate calm", "Easy to do anywhere", "Quick stress relief")
            )
        )
        
        return BuddyActionResult.Success(
            title = "Let's Breathe Together",
            message = "Taking time to breathe is one of the kindest things you can do for yourself. Let's find some calm together.",
            breathingTechniques = breathingTechniques,
            gentleInstructions = listOf(
                "Find a comfortable position",
                "Close your eyes if that feels good",
                "There's no wrong way to do this",
                "Just follow along at your own pace",
                "If your mind wanders, that's completely normal"
            ),
            affirmations = listOf(
                "You are safe in this moment",
                "With each breath, you're taking care of yourself",
                "You deserve this peace and calm"
            )
        )
    }

    private suspend fun practiceGratitude(): BuddyActionResult {
        val gratitudePrompts = listOf(
            "What made you smile today, even for just a moment?",
            "Who in your life are you thankful for right now?",
            "What's something about your body you're grateful for?",
            "What's a small comfort that made your day better?",
            "What's something beautiful you noticed today?",
            "What ability or skill are you grateful to have?",
            "What memory brings you joy when you think about it?"
        )
        
        val gratitudeCategories = mapOf(
            "People" to "Friends, family, mentors, even strangers who were kind",
            "Experiences" to "Adventures, lessons learned, moments of joy",
            "Simple Pleasures" to "Your morning coffee, a cozy bed, sunshine",
            "Personal Growth" to "Challenges overcome, new skills, wisdom gained",
            "Nature" to "Beautiful weather, animals, plants, natural wonders"
        )
        
        return BuddyActionResult.Success(
            title = "Gratitude Moments",
            message = "Gratitude is like sunshine for the soul. Let's shine some light on the good things in your life.",
            gratitudePrompts = gratitudePrompts,
            gratitudeCategories = gratitudeCategories,
            encouragement = listOf(
                "Even tiny things count - like a stranger's smile",
                "It's okay if it's hard to think of things sometimes",
                "You're training your brain to notice the good",
                "Every moment of gratitude is a gift you give yourself"
            ),
            wellnessConnection = "Regular gratitude practice can improve mood, sleep, and overall life satisfaction."
        )
    }

    private suspend fun boostEnergy(): BuddyActionResult {
        val energyBoosters = listOf(
            EnergyBooster(
                activity = "5-Minute Dance Party",
                description = "Put on your favorite song and move your body!",
                timeNeeded = "5 minutes",
                energyLevel = EnergyLevel.HIGH
            ),
            EnergyBooster(
                activity = "Power Pose",
                description = "Stand tall with hands on hips, chest open for 2 minutes",
                timeNeeded = "2 minutes",
                energyLevel = EnergyLevel.MEDIUM
            ),
            EnergyBooster(
                activity = "Gentle Stretching",
                description = "Simple stretches to wake up your body",
                timeNeeded = "3-8 minutes",
                energyLevel = EnergyLevel.LOW
            ),
            EnergyBooster(
                activity = "Positive Affirmations",
                description = "Remind yourself of your strength and worth",
                timeNeeded = "3-5 minutes",
                energyLevel = EnergyLevel.LOW
            ),
            EnergyBooster(
                activity = "Cold Water on Face",
                description = "Splash cool water on your face and wrists",
                timeNeeded = "1 minute",
                energyLevel = EnergyLevel.LOW
            )
        )
        
        return BuddyActionResult.Success(
            title = "Let's Boost Your Energy!",
            message = "Sometimes we all need a little pick-me-up. You deserve to feel vibrant and alive.",
            energyBoosters = energyBoosters,
            personalizedSuggestions = listOf(
                "Choose what feels right for your energy level right now",
                "Even small movements can make a big difference",
                "Remember, low energy days are completely normal",
                "You don't have to feel amazing all the time"
            ),
            motivationalMessages = listOf(
                "You have more strength than you realize",
                "This feeling will pass, and you'll feel better soon",
                "Every small step counts",
                "I believe in you, even when you don't believe in yourself"
            )
        )
    }

    private suspend fun mindfulnessMoment(): BuddyActionResult {
        val mindfulnessExercises = listOf(
            MindfulnessExercise(
                name = "5-4-3-2-1 Grounding",
                description = "Notice 5 things you see, 4 you can touch, 3 you hear, 2 you smell, 1 you taste",
                duration = "3-5 minutes",
                purpose = "Anchor yourself in the present moment"
            ),
            MindfulnessExercise(
                name = "Body Scan",
                description = "Gently notice each part of your body from head to toe",
                duration = "5-15 minutes",
                purpose = "Connect with your physical self with kindness"
            ),
            MindfulnessExercise(
                name = "Mindful Breathing",
                description = "Just notice your breath, without changing it",
                duration = "3-20 minutes",
                purpose = "Find peace in the rhythm of life"
            ),
            MindfulnessExercise(
                name = "Loving-Kindness",
                description = "Send kind thoughts to yourself and others",
                duration = "5-10 minutes",
                purpose = "Cultivate compassion and warmth"
            )
        )
        
        return BuddyActionResult.Success(
            title = "Present Moment Practice",
            message = "Right now is the only moment we truly have. Let's spend it together mindfully.",
            mindfulnessExercises = mindfulnessExercises,
            gentleReminders = listOf(
                "There's no perfect way to be mindful",
                "If your mind wanders, that's completely normal",
                "Be as gentle with yourself as you would a dear friend",
                "Each moment of awareness is a gift"
            ),
            presentMomentPrompts = listOf(
                "What do you notice about this very moment?",
                "How does your body feel right now?",
                "What emotions are present without judging them?",
                "What are you grateful for in this instant?"
            )
        )
    }

    private suspend fun setGoals(): BuddyActionResult {
        val goalCategories = mapOf(
            "Personal Growth" to listOf("Learn new skills", "Build confidence", "Practice self-care"),
            "Health & Wellness" to listOf("Exercise regularly", "Eat nourishing foods", "Get better sleep"),
            "Relationships" to listOf("Connect with friends", "Be more present", "Practice kindness"),
            "Career & Learning" to listOf("Develop skills", "Take on challenges", "Find purpose"),
            "Creativity & Fun" to listOf("Try new hobbies", "Express yourself", "Play more")
        )
        
        return BuddyActionResult.Success(
            title = "Dream Builder Session",
            message = "Your dreams matter, and you deserve to pursue them. Let's create a path toward what brings you joy.",
            goalCategories = goalCategories,
            goalSettingSteps = listOf(
                "Dream big - what would make you truly happy?",
                "Break it down into smaller, manageable steps",
                "Set a realistic timeline that feels good to you",
                "Identify what support or resources you might need",
                "Celebrate every small win along the way"
            ),
            encouragement = listOf(
                "Start where you are, with what you have",
                "Progress isn't always linear - that's perfectly normal",
                "Your goals can change and evolve - that's growth",
                "You don't have to have it all figured out",
                "I'll be here cheering you on every step of the way"
            ),
            smartGoalGuidance = "Make your goals Specific, Measurable, Achievable, Relevant, and Time-bound - but most importantly, make them meaningful to YOU."
        )
    }

    private suspend fun selfCareCheck(): BuddyActionResult {
        val selfCareAreas = listOf(
            SelfCareArea(
                category = "Physical Care",
                activities = listOf("Regular meals", "Adequate sleep", "Movement/exercise", "Medical check-ups"),
                checkInQuestions = listOf("How are you nourishing your body?", "Are you getting enough rest?")
            ),
            SelfCareArea(
                category = "Emotional Care",
                activities = listOf("Journaling", "Therapy/counseling", "Expressing feelings", "Setting boundaries"),
                checkInQuestions = listOf("How are you processing your emotions?", "What boundaries do you need?")
            ),
            SelfCareArea(
                category = "Mental Care",
                activities = listOf("Reading", "Learning", "Meditation", "Reducing negative media"),
                checkInQuestions = listOf("What's feeding your mind positively?", "How is your mental clarity?")
            ),
            SelfCareArea(
                category = "Social Care",
                activities = listOf("Quality time with loved ones", "Meaningful conversations", "Community involvement"),
                checkInQuestions = listOf("How are your relationships?", "Do you feel connected?")
            ),
            SelfCareArea(
                category = "Spiritual Care",
                activities = listOf("Nature time", "Meditation", "Prayer", "Values reflection"),
                checkInQuestions = listOf("What gives your life meaning?", "How do you nurture your spirit?")
            )
        )
        
        return BuddyActionResult.Success(
            title = "Self-Care Check-In",
            message = "Taking care of yourself isn't selfish - it's essential. You deserve to be well and happy.",
            selfCareAreas = selfCareAreas,
            gentleReminders = listOf(
                "Self-care doesn't have to be expensive or time-consuming",
                "Start small - even 5 minutes of self-care counts",
                "What works for others might not work for you, and that's okay",
                "Self-care is different during different seasons of life"
            ),
            selfCompassionNotes = listOf(
                "You're doing the best you can with what you have right now",
                "It's okay if you haven't been taking perfect care of yourself",
                "Every moment is a new opportunity to be kind to yourself",
                "You deserve the same compassion you'd give a good friend"
            )
        )
    }

    private suspend fun relieveStress(): BuddyActionResult {
        val stressReliefTechniques = listOf(
            StressReliefTechnique(
                name = "Progressive Muscle Relaxation",
                description = "Tense and release each muscle group to release physical tension",
                duration = "10-15 minutes",
                effectivenessLevel = EffectivenessLevel.HIGH
            ),
            StressReliefTechnique(
                name = "Worry Time",
                description = "Set aside 15 minutes to write down all your worries, then set them aside",
                duration = "15-20 minutes",
                effectivenessLevel = EffectivenessLevel.MEDIUM
            ),
            StressReliefTechnique(
                name = "Nature Connection",
                description = "Step outside or look at nature, even through a window",
                duration = "5-30 minutes",
                effectivenessLevel = EffectivenessLevel.MEDIUM
            ),
            StressReliefTechnique(
                name = "Creative Expression",
                description = "Draw, write, sing, or create something with your hands",
                duration = "10-60 minutes",
                effectivenessLevel = EffectivenessLevel.HIGH
            )
        )
        
        return BuddyActionResult.Success(
            title = "Stress Relief Session",
            message = "Stress is a normal part of life, but you don't have to carry it alone. Let's find some relief together.",
            stressReliefTechniques = stressReliefTechniques,
            copingStrategies = listOf(
                "Remember that this feeling is temporary",
                "Focus on what you can control right now",
                "Take things one step at a time",
                "It's okay to ask for help when you need it",
                "You've gotten through difficult times before"
            ),
            emergencyContacts = EmergencySupport(
                crisis = "988 (Suicide & Crisis Lifeline)",
                textLine = "Text HOME to 741741 (Crisis Text Line)",
                mentalHealth = "Call your healthcare provider or local mental health services"
            ),
            professionalHelp = "If stress is overwhelming or persistent, consider talking to a counselor or therapist. There's no shame in getting professional support."
        )
    }

    private suspend fun buildConfidence(): BuddyActionResult {
        val confidenceBuilders = listOf(
            "List 3 things you've accomplished recently, no matter how small",
            "Recall a time when someone complimented you - they saw something real",
            "Think of a challenge you overcame - you have that strength within you",
            "Name a quality you like about yourself",
            "Remember a moment when you helped someone else"
        )
        
        val affirmations = listOf(
            "I am worthy of love and respect exactly as I am",
            "I have unique gifts that only I can offer the world",
            "I am learning and growing every day",
            "My feelings and experiences are valid",
            "I deserve good things in my life",
            "I am stronger than I realize",
            "I choose to be kind to myself today"
        )
        
        return BuddyActionResult.Success(
            title = "Confidence Boost Session",
            message = "You are so much more amazing than you realize. Let me remind you of your incredible worth.",
            confidenceBuilders = confidenceBuilders,
            affirmations = affirmations,
            strengthsReminder = listOf(
                "You had the courage to reach out today - that takes strength",
                "You care about growing and improving - that shows wisdom",
                "You're trying to take care of yourself - that demonstrates self-love",
                "You're still here despite everything you've been through - that's resilience"
            ),
            practicalTips = listOf(
                "Keep a list of your accomplishments, big and small",
                "Surround yourself with people who see your worth",
                "Practice speaking to yourself like you would a dear friend",
                "Set small, achievable goals and celebrate when you reach them"
            )
        )
    }

    private suspend fun supportSleep(): BuddyActionResult {
        val sleepTips = listOf(
            SleepTip("Create a bedtime ritual", "Do the same calming activities each night before bed"),
            SleepTip("Limit screens before bed", "Try to avoid screens 1 hour before sleep"),
            SleepTip("Keep your bedroom cool", "Optimal sleep temperature is around 65-68°F"),
            SleepTip("Practice gratitude", "Think of 3 good things from your day"),
            SleepTip("Progressive relaxation", "Relax each part of your body starting with your toes")
        )
        
        val bedtimeRituals = listOf(
            "Gentle stretching or yoga",
            "Reading a calming book",
            "Listening to soothing music",
            "Writing in a journal",
            "Drinking caffeine-free herbal tea",
            "Taking a warm bath or shower"
        )
        
        return BuddyActionResult.Success(
            title = "Sleep Well Support",
            message = "Good sleep is so important for your wellbeing. Let's create a peaceful path to rest.",
            sleepTips = sleepTips,
            bedtimeRituals = bedtimeRituals,
            sleepAffirmations = listOf(
                "I am safe and can rest peacefully",
                "My body knows how to sleep and heal itself",
                "I release the day and welcome rest",
                "Tomorrow is a new day with new possibilities"
            ),
            troubleshooting = mapOf(
                "Racing thoughts" to "Try journaling or a brain dump before bed",
                "Can't fall asleep" to "Get up and do a quiet activity until you feel sleepy",
                "Waking up at night" to "Practice breathing exercises to help you drift back off",
                "Nightmares" to "Consider talking to someone about what might be troubling you"
            )
        )
    }

    private suspend fun friendlyChat(): BuddyActionResult {
        val conversationStarters = listOf(
            "What's been the highlight of your day so far?",
            "Is there anything on your mind that you'd like to talk about?",
            "What's something that made you laugh recently?",
            "Tell me about something you're looking forward to",
            "What's a random thought that's been floating around your head?",
            "If you could do anything right now, what would it be?",
            "What's something you've been curious about lately?"
        )
        
        return BuddyActionResult.Success(
            title = "Let's Chat!",
            message = "I'm so glad you want to spend some time together. I'm here to listen, laugh, or just be present with you.",
            conversationStarters = conversationStarters,
            listeningPromises = listOf(
                "I won't judge anything you share with me",
                "Your thoughts and feelings are always valid",
                "We can talk about anything - big or small",
                "If you need advice, I'll offer it gently",
                "If you just need someone to listen, I'm here for that too"
            ),
            friendshipQualities = listOf(
                "I'll always be patient with you",
                "I believe in you even when you don't believe in yourself",
                "I see the good in you, especially when it's hard for you to see",
                "I'm here for you on good days and challenging days alike"
            )
        )
    }

    private suspend fun celebrate(): BuddyActionResult {
        val celebrationActivities = listOf(
            CelebrationActivity("Victory Dance", "Put on music and dance like nobody's watching!"),
            CelebrationActivity("Treat Yourself", "Get yourself something special - even something small"),
            CelebrationActivity("Share the News", "Tell someone who cares about you"),
            CelebrationActivity("Write it Down", "Document this win in a journal or note"),
            CelebrationActivity("Take a Photo", "Capture this moment to remember later"),
            CelebrationActivity("Gratitude Moment", "Thank yourself for the work you put in")
        )
        
        val congratulationsMessages = listOf(
            "I am SO proud of you! 🎉",
            "Look at you achieving amazing things! ✨",
            "You absolutely deserve to celebrate this! 🌟",
            "This is fantastic news - I'm cheering for you! 📣",
            "You did it! Your hard work paid off! 💪",
            "This calls for a celebration! 🎊"
        )
        
        return BuddyActionResult.Success(
            title = "Time to Celebrate! 🎉",
            message = "Something wonderful happened and I couldn't be happier for you! Let's make sure you truly celebrate this moment.",
            celebrationActivities = celebrationActivities,
            congratulationsMessages = congratulationsMessages,
            celebrationWisdom = listOf(
                "Celebrating your wins, big and small, is so important",
                "You worked hard for this - you deserve to feel proud",
                "Taking time to acknowledge success builds confidence",
                "Your achievements inspire others, even when you don't realize it"
            ),
            reflectionPrompts = listOf(
                "What does this achievement mean to you?",
                "What did you learn about yourself through this process?",
                "How does it feel to reach this milestone?",
                "What would you tell someone else who accomplished this?"
            )
        )
    }

    // Artifact generation methods  
    private suspend fun createPersonalDevelopmentPlan(): BuddyActionResult = withContext(Dispatchers.IO) {
        try {
            val planContent = """
                # Personal Growth Journey
                
                A comprehensive 16-week personal development and self-improvement program.
                
                ## Goals
                - Develop self-awareness and emotional intelligence
                - Build confidence and self-esteem
                - Improve communication and relationship skills
                - Create healthy habits and routines
                - Define and pursue meaningful goals
                
                ## Phase 1: Foundation & Self-Discovery (Weeks 1-4)
                
                ### Module 1: Self-Assessment & Values Clarification
                - Complete personality assessment
                - Write personal mission statement
                - Identify top 5 core values
                Duration: 2-3 hours
                
                ### Module 2: Goal Setting & Vision Creation
                - Write your 5-year vision
                - Set 3 major annual goals
                - Break down goals into quarterly milestones
                Duration: 2-4 hours
                
                ## Phase 2: Skill Development & Habit Formation (Weeks 5-12)
                
                ### Module 3: Communication & Relationship Skills
                - Practice active listening techniques
                - Learn conflict resolution strategies
                - Develop empathy and emotional intelligence
                Duration: 3-5 hours
                
                ### Module 4: Confidence & Self-Esteem Building
                - Challenge negative self-talk
                - Celebrate daily wins and achievements
                - Practice self-compassion exercises
                Duration: 2-3 hours
                
                ## Phase 3: Integration & Mastery (Weeks 13-16)
                
                ### Module 5: Habit Mastery & Routine Optimization
                - Design optimal daily routine
                - Implement habit stacking strategies
                - Create accountability systems
                Duration: 2-3 hours
                
                ### Module 6: Progress Review & Future Planning
                - Complete progress assessment
                - Identify next growth areas
                - Create maintenance plan
                Duration: 1-2 hours
                
                ## Success Metrics
                - Increased self-confidence scores
                - Improved relationship satisfaction
                - Achievement of personal goals
                - Consistent positive habit maintenance
                - Enhanced emotional regulation
            """.trimIndent()

            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = "Personal Growth Journey",
                type = ArtifactType.LEARNING_MODULE,
                description = "A comprehensive 16-week personal development and self-improvement program",
                contentData = ArtifactContent.TextContent(planContent),
                agentType = "buddy",
                userId = "",
                createdAt = System.currentTimeMillis(),
                tags = listOf("personal-development", "self-improvement", "growth", "confidence", "habits")
            )

            BuddyActionResult.Success(
                title = "Personal Development Plan Created! 📈",
                message = "I've created a comprehensive 16-week personal growth journey tailored for holistic development. This structured plan will guide you through self-discovery, skill building, and lasting positive change.",
                encouragement = listOf(
                    "This journey is about progress, not perfection",
                    "Small consistent actions create big transformations",
                    "You have everything within you to grow and succeed",
                    "Every step forward is worth celebrating"
                ),
                practicalTips = listOf(
                    "Start with just 15-20 minutes per day",
                    "Keep a growth journal to track insights",
                    "Find an accountability partner or coach",
                    "Customize the timeline to fit your life",
                    "Remember: sustainable change takes time"
                ),
                celebrationWisdom = listOf(
                    "You've just taken the first step toward transformational growth!",
                    "This comprehensive plan gives you a clear roadmap for success",
                    "Personal development is the best investment you can make",
                    "✨ Your personal growth journey has been created as an artifact!"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating personal development plan", e)
            BuddyActionResult.Error("I encountered an issue creating your personal development plan. Let's try again or explore other growth tools.")
        }
    }

    /* TODO: Fix remaining artifact generation methods
    private suspend fun createWellnessProgram(): BuddyActionResult = withContext(Dispatchers.IO) {
        try {
            val wellnessProgram = WellnessProgram(
                title = "Holistic Wellness & Mental Health Program",
                description = "A comprehensive approach to mental health, emotional well-being, and life balance",
                focus = "Mental health, stress management, and sustainable well-being practices",
                phases = listOf(
                    LearningPhase(
                        phaseNumber = 1,
                        title = "Assessment & Foundation",
                        description = "Understanding your current wellness state and building basics",
                        duration = "Week 1-2",
                        modules = listOf(
                            LearningModule(
                                title = "Mental Health Check-In & Goal Setting",
                                description = "Assess current mental health and define wellness goals",
                                content = "Complete wellness assessment, identify stress triggers and patterns, and set realistic wellness goals",
                                estimatedDuration = "1.5-2 hours"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 2,
                        title = "Core Wellness Practices",
                        description = "Building foundational wellness habits and coping strategies",
                        duration = "Week 3-8",
                        modules = listOf(
                            LearningModule(
                                title = "Stress Management Toolkit",
                                description = "Learn and practice various stress reduction techniques",
                                content = "Master 3 breathing techniques, practice progressive muscle relaxation, and develop personal stress relief protocol",
                                estimatedDuration = "2-3 hours weekly"
                            ),
                            LearningModule(
                                title = "Mindfulness & Present Moment Awareness",
                                description = "Cultivate mindfulness for better mental health",
                                content = "Daily 10-minute mindfulness practice, mindful eating exercises, and body scan meditation",
                                estimatedDuration = "15-30 minutes daily"
                            ),
                            LearningModule(
                                title = "Emotional Regulation & Self-Care",
                                description = "Develop emotional awareness and self-care practices",
                                content = "Emotion tracking and journaling, create personalized self-care menu, and practice self-compassion exercises",
                                estimatedDuration = "1-2 hours weekly"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 3,
                        title = "Integration & Sustainable Practice",
                        description = "Creating lasting wellness habits and support systems",
                        duration = "Week 9-12",
                        modules = listOf(
                            LearningModule(
                                title = "Building Support Networks",
                                description = "Strengthen relationships and build mental health support",
                                content = "Identify your support network, practice asking for help, and create crisis prevention plan",
                                estimatedDuration = "1-2 hours"
                            ),
                            LearningModule(
                                title = "Long-term Wellness Maintenance",
                                description = "Create sustainable practices for ongoing mental health",
                                content = "Design personalized wellness routine, set up regular mental health check-ins, and plan for setback recovery",
                                estimatedDuration = "1.5-2 hours"
                            )
                        )
                    )
                ),
                assessmentCriteria = listOf(
                    "Consistent daily mindfulness practice",
                    "Effective use of stress management tools",
                    "Improved emotional awareness and regulation",
                    "Strong support network activation",
                    "Sustainable self-care routine implementation"
                ),
                successMetrics = listOf(
                    "Reduced stress levels (measured weekly)",
                    "Improved mood stability",
                    "Better sleep quality",
                    "Increased life satisfaction scores",
                    "Enhanced emotional resilience"
                ),
                resources = listOf(
                    "Mental Health Workbook",
                    "Mindfulness & Meditation Guide",
                    "Stress Management Toolkit",
                    "Self-Care Planning Templates",
                    "Crisis Prevention Resources"
                )
            )

            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = wellnessProgram.title,
                type = "wellness-program",
                description = wellnessProgram.description,
                contentData = ArtifactContent.TextContent(json.encodeToString(wellnessProgram)),
                agentType = "buddy",
                userId = "",
                createdAt = System.currentTimeMillis(),
                tags = listOf("wellness", "mental-health", "self-care", "mindfulness", "stress-management")
            )

            BuddyActionResult.Success(
                title = "Wellness Program Created! 🌱",
                message = "I've designed a 12-week holistic wellness program focusing on mental health, stress management, and sustainable well-being. This comprehensive approach will help you build lasting wellness practices.",
                encouragement = listOf(
                    "Your mental health is just as important as your physical health",
                    "Small daily practices create profound long-term changes",
                    "It's okay to have challenging days - healing isn't linear",
                    "You deserve to feel peaceful and balanced"
                ),
                practicalTips = listOf(
                    "Start with just 5-10 minutes of daily practice",
                    "Track your mood and stress levels weekly",
                    "Be patient and compassionate with yourself",
                    "Adjust the program to fit your unique needs",
                    "Consider professional support if needed"
                ),
                celebrationWisdom = listOf(
                    "Taking care of your mental health is an act of self-love!",
                    "This program provides you with professional-grade wellness tools",
                    "You're investing in the most important relationship - the one with yourself",
                    json.encodeToString(artifact)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating wellness program", e)
            BuddyActionResult.Error("I had trouble creating your wellness program. Let's try again or explore other wellness tools.")
        }
    }

    private suspend fun createSkillBuildingJourney(): BuddyActionResult = withContext(Dispatchers.IO) {
        try {
            val skillJourney = SkillBuildingJourney(
                title = "Skill Mastery Learning Path",
                description = "A structured approach to learning and mastering any new skill effectively",
                learningMethodology = "Progressive skill acquisition with deliberate practice and spaced repetition",
                phases = listOf(
                    LearningPhase(
                        phaseNumber = 1,
                        title = "Skill Foundation & Planning",
                        description = "Understanding the skill and creating a learning strategy",
                        duration = "Week 1-2",
                        modules = listOf(
                            LearningModule(
                                title = "Skill Analysis & Goal Definition",
                                description = "Break down the skill and set clear learning objectives",
                                content = "Research skill components and sub-skills, identify current skill level, set specific measurable learning goals, and create success metrics and checkpoints",
                                estimatedDuration = "2-3 hours"
                            ),
                            LearningModule(
                                title = "Learning Strategy Design",
                                description = "Choose optimal learning methods and create study schedule",
                                content = "Select learning resources, design practice schedule with spaced repetition, and set up feedback and assessment systems",
                                estimatedDuration = "1-2 hours"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 2,
                        title = "Active Learning & Practice",
                        description = "Intensive skill development through deliberate practice",
                        duration = "Week 3-10",
                        modules = listOf(
                            LearningModule(
                                title = "Fundamentals Mastery",
                                description = "Master the core principles and basic techniques",
                                content = "Daily focused practice sessions, complete foundational exercises and drills, seek feedback from mentors or peers, and document learning insights and challenges",
                                estimatedDuration = "5-10 hours weekly"
                            ),
                            LearningModule(
                                title = "Progressive Challenge & Application",
                                description = "Apply skills in increasingly complex scenarios",
                                content = "Take on progressively difficult projects, apply skills in real-world contexts, learn from mistakes and failures, and seek diverse practice opportunities",
                                estimatedDuration = "6-12 hours weekly"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 3,
                        title = "Mastery & Integration",
                        description = "Refining skills and developing expertise",
                        duration = "Week 11-16",
                        modules = listOf(
                            LearningModule(
                                title = "Advanced Techniques & Specialization",
                                description = "Develop advanced capabilities and find your specialty",
                                content = "Learn advanced techniques and methods, develop personal style or approach, explore specialization areas, and teach others to reinforce learning",
                                estimatedDuration = "4-8 hours weekly"
                            ),
                            LearningModule(
                                title = "Skill Integration & Maintenance",
                                description = "Integrate skills into daily life and plan continued growth",
                                content = "Create skill maintenance routine, integrate skills into work/life contexts, plan continued learning pathway, and share knowledge with others",
                                estimatedDuration = "2-4 hours weekly"
                            )
                        )
                    )
                ),
                assessmentCriteria = listOf(
                    "Demonstration of core skill competencies",
                    "Completion of progressively complex projects",
                    "Consistent practice schedule maintenance",
                    "Ability to teach or explain the skill to others",
                    "Integration of skill into real-world applications"
                ),
                successMetrics = listOf(
                    "Achievement of defined skill milestones",
                    "Consistent improvement in practice assessments",
                    "Successful completion of skill-based projects",
                    "Positive feedback from mentors or peers",
                    "Ability to help others learn the skill"
                ),
                resources = listOf(
                    "Skill Development Workbook",
                    "Practice Planning Templates", 
                    "Progress Tracking Tools",
                    "Feedback Collection Forms",
                    "Learning Resource Database"
                )
            )

            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = skillJourney.title,
                type = "skill-building-journey",
                description = skillJourney.description,
                contentData = ArtifactContent.TextContent(json.encodeToString(skillJourney)),
                agentType = "buddy",
                userId = "",
                createdAt = System.currentTimeMillis(),
                tags = listOf("skill-building", "learning", "mastery", "practice", "development")
            )

            BuddyActionResult.Success(
                title = "Skill Mastery Path Created! 🎯",
                message = "I've created a comprehensive 16-week skill development program that will help you master any new skill effectively. This research-based approach uses deliberate practice and progressive challenge for optimal learning.",
                encouragement = listOf(
                    "Every expert was once a beginner - you're on the right path",
                    "Consistent practice beats sporadic intensity every time",
                    "Mistakes and failures are essential parts of learning",
                    "You have the capacity to develop remarkable skills"
                ),
                practicalTips = listOf(
                    "Choose one specific skill to focus on initially",
                    "Practice daily, even if just for 20-30 minutes",
                    "Track your progress to stay motivated",
                    "Seek feedback regularly from others",
                    "Celebrate small wins along the way"
                ),
                celebrationWisdom = listOf(
                    "You're investing in yourself - the best investment possible!",
                    "This structured approach gives you a proven path to mastery",
                    "Skills compound over time - you're building your future self",
                    json.encodeToString(artifact)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating skill building journey", e)
            BuddyActionResult.Error("I ran into an issue creating your skill building journey. Let's try another approach or explore different learning tools.")
        }
    }

    private suspend fun createHabitFormationGuide(): BuddyActionResult = withContext(Dispatchers.IO) {
        try {
            val habitGuide = HabitFormationGuide(
                title = "Science-Based Habit Formation Program",
                description = "A systematic approach to building lasting positive habits and breaking negative ones",
                methodology = "Based on behavior science, habit loop psychology, and progressive implementation",
                phases = listOf(
                    LearningPhase(
                        phaseNumber = 1,
                        title = "Habit Science & Preparation",
                        description = "Understanding habit formation and preparing for change",
                        duration = "Week 1",
                        modules = listOf(
                            LearningModule(
                                title = "The Science of Habits",
                                description = "Learn how habits form and how to leverage psychology for success",
                                content = "Study the habit loop, identify your existing habit patterns, learn about neural pathways and habit automation, and understand keystone habits and habit stacking",
                                estimatedDuration = "2-3 hours"
                            ),
                            LearningModule(
                                title = "Habit Selection & Goal Setting",
                                description = "Choose the right habits and set up for success",
                                content = "Identify 1-3 priority habits to focus on, make habits specific small and measurable, link new habits to existing routines, and design your habit environment for success",
                                estimatedDuration = "1-2 hours"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 2,
                        title = "Implementation & Consistency Building",
                        description = "Building new habits through consistent practice",
                        duration = "Week 2-9",
                        modules = listOf(
                            LearningModule(
                                title = "The 2-Minute Rule & Starting Small",
                                description = "Begin with tiny habits that are impossible to fail",
                                content = "Start with 2-minute versions of target habits, focus on showing up consistently vs. performance, track completion not quality initially, and celebrate small wins every day",
                                estimatedDuration = "5-15 minutes daily"
                            ),
                            LearningModule(
                                title = "Habit Stacking & Environmental Design",
                                description = "Use existing habits and environment to trigger new ones",
                                content = "Stack new habits onto existing routines, design physical environment to support habits, remove friction for good habits, and add friction for bad habits",
                                estimatedDuration = "30 minutes setup, 5-20 minutes daily"
                            ),
                            LearningModule(
                                title = "Dealing with Setbacks & Obstacles",
                                description = "Overcome common habit formation challenges",
                                content = "Plan for common obstacles and setbacks, develop bounce-back strategies, practice self-compassion during lapses, and adjust habits based on what you learn",
                                estimatedDuration = "1 hour planning, ongoing application"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 3,
                        title = "Automation & Advanced Strategies",
                        description = "Making habits automatic and building advanced systems",
                        duration = "Week 10-12",
                        modules = listOf(
                            LearningModule(
                                title = "Habit Automation & Scaling",
                                description = "Make habits automatic and gradually increase complexity",
                                content = "Gradually increase habit difficulty/duration, build habit chains and morning/evening routines, create habit-supporting social environment, and develop habit maintenance systems",
                                estimatedDuration = "20-60 minutes daily"
                            ),
                            LearningModule(
                                title = "Long-term Habit Mastery",
                                description = "Create sustainable habit systems for life",
                                content = "Regular habit review and optimization, seasonal habit adjustments, teaching habits to others, and building habit-supportive identity",
                                estimatedDuration = "1 hour weekly review"
                            )
                        )
                    )
                ),
                assessmentCriteria = listOf(
                    "Consistent habit performance for 66+ days",
                    "Successful navigation of at least 3 setbacks",
                    "Automatic habit performance without conscious effort",
                    "Successful habit stacking with existing routines",
                    "Ability to help others build similar habits"
                ),
                successMetrics = listOf(
                    "90%+ habit completion rate over 30 days",
                    "Reduced mental effort to perform habits",
                    "Automatic habit performance in various contexts",
                    "Positive identity shifts related to habits",
                    "Successful habit maintenance during stress"
                ),
                resources = listOf(
                    "Habit Formation Workbook",
                    "Digital Habit Tracker",
                    "Environmental Design Guide",
                    "Setback Recovery Protocols",
                    "Habit Science Reference Materials"
                )
            )

            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = habitGuide.title,
                type = "habit-formation-guide",
                description = habitGuide.description,
                contentData = ArtifactContent.TextContent(json.encodeToString(habitGuide)),
                agentType = "buddy",
                userId = "",
                createdAt = System.currentTimeMillis(),
                tags = listOf("habits", "behavior-change", "routine", "consistency", "psychology")
            )

            BuddyActionResult.Success(
                title = "Habit Formation Guide Created! 🔄",
                message = "I've designed a 12-week science-based habit formation program that will help you build lasting positive habits. This systematic approach uses proven behavioral psychology for maximum success.",
                encouragement = listOf(
                    "Habits are the compound interest of self-improvement",
                    "Small changes create remarkable results over time",
                    "You don't have to be perfect, just consistent",
                    "Every day is a new opportunity to strengthen your habits"
                ),
                practicalTips = listOf(
                    "Start with just ONE habit to avoid overwhelming yourself",
                    "Make your habit so small it's hard to say no",
                    "Focus on showing up consistently, not perfection",
                    "Track your habits visually to stay motivated",
                    "Celebrate every success, no matter how small"
                ),
                celebrationWisdom = listOf(
                    "You're creating the building blocks of an extraordinary life!",
                    "This program gives you the tools to change any behavior permanently",
                    "Habits shape identity - you're becoming your best self",
                    json.encodeToString(artifact)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating habit formation guide", e)
            BuddyActionResult.Error("I had difficulty creating your habit formation guide. Let's try again or explore other behavior change tools.")
        }
    }

    private suspend fun createMindfulnessProgram(): BuddyActionResult = withContext(Dispatchers.IO) {
        try {
            val mindfulnessProgram = MindfulnessProgram(
                title = "Mindfulness & Meditation Practice Program",
                description = "A comprehensive journey into mindfulness, meditation, and present-moment awareness",
                practiceStyle = "Secular, accessible mindfulness with multiple meditation techniques",
                phases = listOf(
                    LearningPhase(
                        phaseNumber = 1,
                        title = "Mindfulness Foundation",
                        description = "Introduction to mindfulness concepts and basic practices",
                        duration = "Week 1-3",
                        modules = listOf(
                            LearningModule(
                                title = "Understanding Mindfulness",
                                description = "Learn the principles and benefits of mindfulness practice",
                                content = "Study mindfulness definition and core principles, learn about present-moment awareness, understand mindfulness vs. meditation, and complete mindfulness assessment",
                                estimatedDuration = "1-2 hours"
                            ),
                            LearningModule(
                                title = "Breathing & Body Awareness",
                                description = "Develop basic breath and body awareness practices",
                                content = "Practice basic breath awareness daily, learn body scan meditation, practice mindful breathing during daily activities, and develop anchor breath technique",
                                estimatedDuration = "10-15 minutes daily"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 2,
                        title = "Core Meditation Practices",
                        description = "Developing regular meditation practice and mindful awareness",
                        duration = "Week 4-10",
                        modules = listOf(
                            LearningModule(
                                title = "Sitting Meditation Practice",
                                description = "Establish regular sitting meditation routine",
                                content = "Daily sitting meditation (start 5 min, build to 20 min), practice working with thoughts and distractions, learn loving-kindness meditation, and develop personal meditation space",
                                estimatedDuration = "10-25 minutes daily"
                            ),
                            LearningModule(
                                title = "Mindful Daily Living",
                                description = "Integrate mindfulness into everyday activities",
                                content = "Practice mindful eating, walking, and listening, use mindfulness during routine tasks, apply mindfulness to difficult emotions, and practice mindful communication",
                                estimatedDuration = "Throughout daily activities"
                            ),
                            LearningModule(
                                title = "Working with Difficult States",
                                description = "Use mindfulness for stress, anxiety, and challenging emotions",
                                content = "Practice RAIN technique, use mindfulness for anxiety and stress, practice non-judgmental awareness, and learn mindful self-compassion",
                                estimatedDuration = "15-20 minutes daily practice"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 3,
                        title = "Advanced Practice & Integration",
                        description = "Deepening practice and making mindfulness a way of life",
                        duration = "Week 11-12",
                        modules = listOf(
                            LearningModule(
                                title = "Deepening Your Practice",
                                description = "Explore advanced techniques and longer practices",
                                content = "Try longer meditation sessions (30-45 minutes), explore different meditation styles, practice mindful movement, and consider meditation retreat experience",
                                estimatedDuration = "30-45 minutes daily"
                            ),
                            LearningModule(
                                title = "Sustainable Practice & Teaching",
                                description = "Create sustainable long-term practice and share with others",
                                content = "Design your personal mindfulness routine, plan for maintaining practice during busy periods, share mindfulness with family/friends, and continue learning through books/courses",
                                estimatedDuration = "Ongoing maintenance"
                            )
                        )
                    )
                ),
                assessmentCriteria = listOf(
                    "Consistent daily practice for 8+ weeks",
                    "Ability to return attention to present moment",
                    "Reduced reactivity to stressful situations",
                    "Increased self-compassion and acceptance",
                    "Integration of mindfulness into daily life"
                ),
                successMetrics = listOf(
                    "Improved attention and focus",
                    "Reduced anxiety and stress levels",
                    "Enhanced emotional regulation",
                    "Increased life satisfaction and well-being",
                    "Greater sense of inner peace"
                ),
                resources = listOf(
                    "Mindfulness Practice Workbook",
                    "Meditation Timer & Bell App",
                    "Guided Meditation Library",
                    "Daily Practice Tracker",
                    "Mindfulness Reading List"
                )
            )

            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = mindfulnessProgram.title,
                type = "mindfulness-program",
                description = mindfulnessProgram.description,
                contentData = ArtifactContent.TextContent(json.encodeToString(mindfulnessProgram)),
                agentType = "buddy",
                userId = "",
                createdAt = System.currentTimeMillis(),
                tags = listOf("mindfulness", "meditation", "awareness", "stress-relief", "peace")
            )

            BuddyActionResult.Success(
                title = "Mindfulness Program Created! 🧘",
                message = "I've created a comprehensive 12-week mindfulness and meditation program that will help you cultivate present-moment awareness and inner peace. This accessible approach works for complete beginners and experienced practitioners alike.",
                encouragement = listOf(
                    "The present moment is the only time you truly have",
                    "Mindfulness is not about stopping thoughts, but changing your relationship with them",
                    "Every moment of awareness is valuable, regardless of how brief",
                    "You already have everything you need within you"
                ),
                practicalTips = listOf(
                    "Start with just 3-5 minutes of daily practice",
                    "Find a quiet space where you won't be disturbed",
                    "Be patient and kind with yourself as you learn",
                    "Use a meditation app or timer to help you stay consistent",
                    "Remember: there's no such thing as a 'bad' meditation"
                ),
                celebrationWisdom = listOf(
                    "You're embarking on one of humanity's most valuable practices!",
                    "This program offers you tools for lifelong peace and well-being",
                    "Mindfulness is a gift you give yourself every single day",
                    json.encodeToString(artifact)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating mindfulness program", e)
            BuddyActionResult.Error("I encountered an issue creating your mindfulness program. Let's try again or explore other mindfulness tools.")
        }
    }

    private suspend fun createConfidenceWorkshop(): BuddyActionResult = withContext(Dispatchers.IO) {
        try {
            val confidenceWorkshop = ConfidenceWorkshop(
                title = "Comprehensive Confidence Building Workshop",
                description = "A systematic approach to building unshakeable self-confidence and self-esteem",
                methodology = "Cognitive-behavioral techniques, positive psychology, and practical confidence-building exercises",
                phases = listOf(
                    LearningPhase(
                        phaseNumber = 1,
                        title = "Confidence Assessment & Foundation",
                        description = "Understanding current confidence levels and building awareness",
                        duration = "Week 1-2",
                        modules = listOf(
                            LearningModule(
                                title = "Confidence Self-Assessment",
                                description = "Assess current confidence levels and identify growth areas",
                                content = "Complete comprehensive confidence assessment, identify specific situations where confidence is lacking, recognize current strengths and achievements, and map confidence patterns and triggers",
                                estimatedDuration = "1.5-2 hours"
                            ),
                            LearningModule(
                                title = "Understanding Confidence Psychology",
                                description = "Learn how confidence works and what undermines it",
                                content = "Study the difference between confidence and self-esteem, learn about imposter syndrome and self-doubt, understand the confidence-competence loop, and identify limiting beliefs and negative self-talk",
                                estimatedDuration = "1-2 hours"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 2,
                        title = "Core Confidence Building",
                        description = "Active confidence building through mindset work and behavioral changes",
                        duration = "Week 3-8",
                        modules = listOf(
                            LearningModule(
                                title = "Challenging Negative Thoughts",
                                description = "Learn to identify and challenge confidence-undermining thoughts",
                                content = "Practice thought catching and analysis, learn cognitive restructuring techniques, develop balanced realistic self-talk, and create personal affirmations and mantras",
                                estimatedDuration = "15-20 minutes daily"
                            ),
                            LearningModule(
                                title = "Building Evidence of Competence",
                                description = "Create concrete evidence of your abilities and worth",
                                content = "Keep an achievement and success journal, document compliments and positive feedback, take on small challenges to build competence, and create a confidence portfolio of accomplishments",
                                estimatedDuration = "10-15 minutes daily"
                            ),
                            LearningModule(
                                title = "Body Language & Presence",
                                description = "Develop confident body language and physical presence",
                                content = "Practice confident posture and movement, learn power posing techniques, work on voice tone and speaking confidence, and develop a confident personal style",
                                estimatedDuration = "10-20 minutes daily practice"
                            )
                        )
                    ),
                    LearningPhase(
                        phaseNumber = 3,
                        title = "Social Confidence & Integration",
                        description = "Building confidence in social situations and maintaining progress",
                        duration = "Week 9-12",
                        modules = listOf(
                            LearningModule(
                                title = "Social Confidence Skills",
                                description = "Build confidence in interpersonal interactions",
                                content = "Practice conversation starters and small talk, learn assertiveness techniques, practice handling criticism and rejection, and develop networking and relationship-building skills",
                                estimatedDuration = "20-30 minutes practice + real-world application"
                            ),
                            LearningModule(
                                title = "Confidence Maintenance & Growth",
                                description = "Create systems for maintaining and growing confidence long-term",
                                content = "Design daily confidence-building routine, create support network for ongoing growth, plan regular confidence challenges, and develop resilience for setbacks",
                                estimatedDuration = "1 hour planning, 10-15 minutes daily maintenance"
                            )
                        )
                    )
                ),
                assessmentCriteria = listOf(
                    "Improved self-assessment scores across confidence areas",
                    "Successful completion of confidence challenges",
                    "Reduced negative self-talk and increased positive self-talk",
                    "Confident body language and presence",
                    "Improved social interaction comfort and skills"
                ),
                successMetrics = listOf(
                    "20%+ increase in confidence assessment scores",
                    "Successful navigation of previously avoided situations",
                    "Positive feedback from others about increased confidence",
                    "Reduced anxiety in confidence-challenging situations",
                    "Increased willingness to take on new challenges"
                ),
                resources = listOf(
                    "Confidence Building Workbook",
                    "Daily Affirmation Cards",
                    "Achievement & Success Journal",
                    "Social Skills Practice Guide",
                    "Confidence Maintenance Toolkit"
                )
            )

            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = confidenceWorkshop.title,
                type = "confidence-workshop",
                description = confidenceWorkshop.description,
                contentData = ArtifactContent.TextContent(json.encodeToString(confidenceWorkshop)),
                agentType = "buddy",
                userId = "",
                createdAt = System.currentTimeMillis(),
                tags = listOf("confidence", "self-esteem", "assertiveness", "social-skills", "empowerment")
            )

            BuddyActionResult.Success(
                title = "Confidence Workshop Created! ⭐",
                message = "I've designed a comprehensive 12-week confidence building workshop that will help you develop unshakeable self-confidence. This program combines psychology-based techniques with practical exercises for lasting change.",
                encouragement = listOf(
                    "Confidence is a skill that can be learned and developed",
                    "You are far more capable than you realize",
                    "Every small step forward builds your confidence muscle",
                    "Your worth is not determined by others' opinions"
                ),
                practicalTips = listOf(
                    "Start with small, achievable confidence challenges",
                    "Track your progress and celebrate every win",
                    "Practice confident body language even when you don't feel confident",
                    "Surround yourself with supportive, positive people",
                    "Remember: confidence comes from action, not feeling"
                ),
                celebrationWisdom = listOf(
                    "You've just taken a brave step toward becoming your most confident self!",
                    "This workshop provides you with proven tools for lasting confidence",
                    "Confidence is your birthright - you deserve to feel amazing about yourself",
                    json.encodeToString(artifact)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating confidence workshop", e)
            BuddyActionResult.Error("I ran into trouble creating your confidence workshop. Let's try again or explore other confidence-building tools.")
        }
    } */

    fun cleanup() {
        scope.cancel()
    }
}


// Data models for Buddy tools
data class BuddyTool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val action: suspend () -> BuddyActionResult,
    val category: BuddyCategory,
    val duration: String,
    val benefits: List<String>,
    val suitableFor: List<String> = listOf("Everyone")
)

enum class BuddyCategory {
    EMOTIONAL_SUPPORT, RELAXATION, POSITIVITY, MOTIVATION, MINDFULNESS, 
    GROWTH, SELF_CARE, STRESS_MANAGEMENT, CONFIDENCE, SLEEP, 
    COMPANIONSHIP, CELEBRATION
}

sealed class BuddyActionResult {
    data class Success(
        val title: String,
        val message: String,
        val moodOptions: List<MoodOption> = emptyList(),
        val followUpQuestions: List<String> = emptyList(),
        val encouragement: List<String> = emptyList(),
        val breathingTechniques: List<BreathingTechnique> = emptyList(),
        val gentleInstructions: List<String> = emptyList(),
        val affirmations: List<String> = emptyList(),
        val gratitudePrompts: List<String> = emptyList(),
        val gratitudeCategories: Map<String, String> = emptyMap(),
        val wellnessConnection: String = "",
        val energyBoosters: List<EnergyBooster> = emptyList(),
        val personalizedSuggestions: List<String> = emptyList(),
        val motivationalMessages: List<String> = emptyList(),
        val mindfulnessExercises: List<MindfulnessExercise> = emptyList(),
        val gentleReminders: List<String> = emptyList(),
        val presentMomentPrompts: List<String> = emptyList(),
        val goalCategories: Map<String, List<String>> = emptyMap(),
        val goalSettingSteps: List<String> = emptyList(),
        val smartGoalGuidance: String = "",
        val selfCareAreas: List<SelfCareArea> = emptyList(),
        val selfCompassionNotes: List<String> = emptyList(),
        val stressReliefTechniques: List<StressReliefTechnique> = emptyList(),
        val copingStrategies: List<String> = emptyList(),
        val emergencyContacts: EmergencySupport? = null,
        val professionalHelp: String = "",
        val confidenceBuilders: List<String> = emptyList(),
        val strengthsReminder: List<String> = emptyList(),
        val practicalTips: List<String> = emptyList(),
        val sleepTips: List<SleepTip> = emptyList(),
        val bedtimeRituals: List<String> = emptyList(),
        val sleepAffirmations: List<String> = emptyList(),
        val troubleshooting: Map<String, String> = emptyMap(),
        val conversationStarters: List<String> = emptyList(),
        val listeningPromises: List<String> = emptyList(),
        val friendshipQualities: List<String> = emptyList(),
        val celebrationActivities: List<CelebrationActivity> = emptyList(),
        val congratulationsMessages: List<String> = emptyList(),
        val celebrationWisdom: List<String> = emptyList(),
        val reflectionPrompts: List<String> = emptyList()
    ) : BuddyActionResult()
    
    data class Error(val message: String) : BuddyActionResult()
}

data class MoodOption(
    val emoji: String,
    val label: String,
    val value: Int,
    val color: androidx.compose.ui.graphics.Color
)

@Serializable
data class BreathingTechnique(
    val name: String,
    val description: String,
    val duration: String,
    val benefits: List<String>
)

@Serializable
data class EnergyBooster(
    val activity: String,
    val description: String,
    val timeNeeded: String,
    val energyLevel: EnergyLevel
)

@Serializable
enum class EnergyLevel {
    LOW, MEDIUM, HIGH
}

@Serializable
data class MindfulnessExercise(
    val name: String,
    val description: String,
    val duration: String,
    val purpose: String
)

@Serializable
data class SelfCareArea(
    val category: String,
    val activities: List<String>,
    val checkInQuestions: List<String>
)

@Serializable
data class StressReliefTechnique(
    val name: String,
    val description: String,
    val duration: String,
    val effectivenessLevel: EffectivenessLevel
)

@Serializable
enum class EffectivenessLevel {
    LOW, MEDIUM, HIGH
}

@Serializable
data class EmergencySupport(
    val crisis: String,
    val textLine: String,
    val mentalHealth: String
)

@Serializable
data class SleepTip(
    val title: String,
    val description: String
)

@Serializable
data class CelebrationActivity(
    val name: String,
    val description: String
)

@Serializable
data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val moodValue: Int, // 1-5
    val moodLabel: String,
    val notes: String = "",
    val triggers: List<String> = emptyList(),
    val gratitude: List<String> = emptyList()
)

@Serializable
data class WellnessGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String,
    val targetDate: Long,
    val progress: Float = 0f,
    val milestones: List<String>,
    val isActive: Boolean = true,
    val createdDate: Long = System.currentTimeMillis()
)

@Serializable
data class SupportSession(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val toolUsed: String,
    val duration: Long, // milliseconds
    val mood: MoodEntry?,
    val notes: String = "",
    val helpfulness: Int? = null, // 1-5 rating
    val followUp: String = ""
)