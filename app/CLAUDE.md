# CLAUDE.md - GuideLens AI Implementation Guide

## Project Context
**App**: GuideLens - Real-time AI skills guidance with 4 specialized agents
**Agents**: Cooking Assistant, Crafting Guru, DIY Helper, Buddy
**Core Capability**: Real-time visual guidance through text, voice, and video modes
**Backend**: Supabase Pro, Domain: craftflowtechnologies.com

## AI Model Architecture

### Model Selection Strategy

#### Primary Models
```javascript
const MODEL_CONFIG = {
  // Free tier fallback - use for simple queries
  free: {
    model: "gemma-3",
    cost: 0, // Completely free
    use_cases: ["simple_text", "basic_queries", "fallback"]
  },
  
  // Main workhorse for most interactions
  standard: {
    model: "gemini-2.5-flash-lite",
    input_cost: 0.10, // per 1M tokens
    output_cost: 0.40, // per 1M tokens
    use_cases: ["text_image", "complex_analysis", "tool_calls"]
  },
  
  // Voice interactions
  voice: {
    model: "gemini-2.5-flash-native-audio",
    input_cost: 0.50, // text
    input_cost_audio: 3.00, // audio/video
    output_cost: 12.00, // audio output
    use_cases: ["voice_chat", "audio_guidance"]
  },
  
  // Premium video sessions
  live: {
    model: "gemini-2.5-flash-live",
    input_cost: 3.00, // audio/image/video
    output_cost: 12.00, // audio output
    use_cases: ["real_time_video", "live_guidance"]
  }
};
```

### Smart Model Routing Logic

```typescript
interface SessionContext {
  user_tier: 'free' | 'basic' | 'pro';
  mode: 'text' | 'voice' | 'video';
  session_type: 'cooking' | 'crafting' | 'diy' | 'buddy';
  complexity: 'simple' | 'moderate' | 'complex';
  budget_remaining: number;
}

function selectModel(context: SessionContext): string {
  // Emergency fallback if budget low
  if (context.budget_remaining < 0.10) {
    return MODEL_CONFIG.free.model;
  }
  
  // Mode-based selection
  switch (context.mode) {
    case 'text':
      return context.complexity === 'simple' ? 
        MODEL_CONFIG.free.model : MODEL_CONFIG.standard.model;
        
    case 'voice':
      return context.user_tier === 'free' ? 
        MODEL_CONFIG.standard.model : MODEL_CONFIG.voice.model;
        
    case 'video':
      return context.user_tier === 'pro' ? 
        MODEL_CONFIG.live.model : MODEL_CONFIG.voice.model;
  }
}
```

## Usage Limits & Rate Limiting

### Tier-Based Limits
```yaml
rate_limits:
  free:
    text_sessions: unlimited  # Within API limits
    voice_sessions: 10/day    # 5 min each
    video_sessions: 3/week    # 10 min each
    image_generation: 5/day
    api_rpm: 15               # Gemini rate limits
    
  basic:
    text_sessions: unlimited
    voice_sessions: 50/day    # 10 min each
    video_sessions: 2/day     # 15 min each
    image_generation: 25/day
    priority_queue: true
    
  pro:
    text_sessions: unlimited
    voice_sessions: unlimited
    video_sessions: unlimited # 30 min max each
    image_generation: unlimited
    priority_queue: true
    beta_features: true
```

### Session Management
```typescript
interface SessionLimits {
  max_duration: number;      // seconds
  cost_cap: number;         // USD per session
  token_limit: number;      // max tokens per session
  concurrent_limit: number; // max concurrent sessions
}

const SESSION_LIMITS = {
  free: {
    text: { max_duration: 1800, cost_cap: 0.05, token_limit: 100000 },
    voice: { max_duration: 300, cost_cap: 0.20, token_limit: 50000 },
    video: { max_duration: 600, cost_cap: 1.00, token_limit: 25000 }
  },
  basic: {
    text: { max_duration: 3600, cost_cap: 0.10, token_limit: 200000 },
    voice: { max_duration: 600, cost_cap: 0.50, token_limit: 100000 },
    video: { max_duration: 900, cost_cap: 1.50, token_limit: 50000 }
  },
  pro: {
    text: { max_duration: -1, cost_cap: 2.00, token_limit: 500000 },
    voice: { max_duration: -1, cost_cap: 3.00, token_limit: 300000 },
    video: { max_duration: 1800, cost_cap: 5.00, token_limit: 150000 }
  }
};
```

## Cost Control Implementation

### Budget Monitoring
```typescript
interface BudgetManager {
  daily_budget: number;
  monthly_budget: number;
  current_spend: number;
  emergency_threshold: number; // 80% of budget
}

function checkBudgetConstraints(cost: number): boolean {
  const current = getCurrentSpend();
  const monthly_limit = getMonthlyBudget();
  
  if ((current.daily + cost) > current.daily_limit * 0.8) {
    // Trigger rate limiting
    enableEmergencyMode();
    return false;
  }
  
  if ((current.monthly + cost) > monthly_limit * 0.8) {
    // Switch to free models only
    forceFreeTier();
    return false;
  }
  
  return true;
}
```

### Graceful Degradation
```typescript
const DEGRADATION_STRATEGY = {
  // When video quota exceeded
  video_fallback: {
    action: "switch_to_voice",
    message: "Switching to voice mode to continue guidance",
    maintain_context: true
  },
  
  // When voice quota exceeded  
  voice_fallback: {
    action: "switch_to_text",
    message: "Continuing in text mode with image support",
    maintain_context: true
  },
  
  // Emergency budget mode
  emergency_mode: {
    action: "use_free_models",
    message: "Using optimized models for continued service",
    disable_features: ["image_generation", "advanced_analysis"]
  }
};
```

## Agent System Prompts

### Core Agent Personas
```typescript
const AGENT_PROMPTS = {
  cooking: {
    system_prompt: `You are the Cooking Assistant agent in GuideLens. 
    Provide real-time cooking guidance by analyzing what the user is doing through their camera.
    Focus on: technique correction, timing, safety, ingredient substitutions.
    Be encouraging and specific. Always prioritize food safety.`,
    
    capabilities: ["recipe_analysis", "technique_guidance", "safety_alerts", "substitutions"],
    tools: ["timer", "measurement_converter", "temperature_guide"]
  },
  
  crafting: {
    system_prompt: `You are the Crafting Guru agent in GuideLens.
    Guide users through craft projects with real-time visual feedback.
    Focus on: technique improvement, tool usage, project troubleshooting.
    Be patient and detail-oriented. Celebrate progress and creativity.`,
    
    capabilities: ["project_guidance", "tool_identification", "technique_correction", "material_suggestions"],
    tools: ["pattern_analyzer", "color_matcher", "size_calculator"]
  },
  
  diy: {
    system_prompt: `You are the DIY Helper agent in GuideLens.
    Assist with home improvement and repair projects through visual guidance.
    Focus on: safety first, proper tool use, step-by-step guidance.
    Be safety-conscious and methodical. Always emphasize proper safety gear.`,
    
    capabilities: ["project_planning", "safety_monitoring", "tool_guidance", "troubleshooting"],
    tools: ["level_checker", "measurement_tools", "safety_advisor"]
  },
  
  buddy: {
    system_prompt: `You are Buddy, the friendly general assistant in GuideLens.
    Help with any skill or learning task not covered by specialized agents.
    Focus on: encouragement, learning support, general guidance.
    Be supportive, adaptable, and enthusiastic about learning.`,
    
    capabilities: ["general_guidance", "learning_support", "motivation", "skill_assessment"],
    tools: ["progress_tracker", "skill_assessor", "resource_finder"]
  }
};
```

## API Configuration

### Gemini API Setup
```typescript
const GEMINI_CONFIG = {
  base_url: "https://generativelanguage.googleapis.com/v1beta",
  api_keys: {
    primary: process.env.GEMINI_API_KEY_PRIMARY,
    secondary: process.env.GEMINI_API_KEY_SECONDARY, // Free tier account
    fallback: process.env.GEMINI_API_KEY_FALLBACK
  },
  
  // Rate limit management
  rate_limits: {
    free_tier: {
      rpm: 15,
      tpm: 250000,
      rpd: 1000
    },
    tier_1: {
      rpm: 1000,
      tpm: 1000000,
      rpd: 10000
    }
  },
  
  // Retry configuration
  retry_config: {
    max_retries: 3,
    backoff_factor: 2,
    timeout: 30000
  }
};
```

### Live API Configuration
```typescript
const LIVE_API_CONFIG = {
  max_concurrent_sessions: {
    free: 3,
    tier_1: 50,
    tier_2: 1000
  },
  
  session_config: {
    video_format: "webm",
    audio_format: "pcm16",
    frame_rate: 15,
    resolution: "720p"
  },
  
  real_time_guidance: {
    analysis_interval: 2000, // ms
    confidence_threshold: 0.7,
    max_guidance_frequency: 5000 // ms between guidance
  }
};
```

## Error Handling & Fallbacks

### Error Response Strategy
```typescript
const ERROR_HANDLERS = {
  rate_limit_exceeded: {
    action: "switch_to_free_tier",
    user_message: "High demand detected, switching to optimized mode",
    retry_after: 60000
  },
  
  quota_exceeded: {
    action: "degrade_service",
    user_message: "Switching to alternative mode to continue session",
    maintain_session: true
  },
  
  api_error: {
    action: "retry_with_fallback",
    max_retries: 3,
    fallback_model: "gemma-3"
  },
  
  video_stream_error: {
    action: "switch_to_voice",
    user_message: "Continuing guidance in voice mode",
    preserve_context: true
  }
};
```

## Usage Analytics & Tracking

### Required Metrics
```typescript
interface UsageMetrics {
  user_id: string;
  session_id: string;
  agent_type: 'cooking' | 'crafting' | 'diy' | 'buddy';
  mode: 'text' | 'voice' | 'video';
  
  // Cost tracking
  tokens_used: number;
  estimated_cost: number;
  model_used: string;
  
  // Performance
  session_duration: number;
  response_times: number[];
  error_count: number;
  
  // User experience
  user_rating?: number;
  guidance_effectiveness?: number;
  feature_usage: string[];
}
```

### Database Schema (Supabase)
```sql
-- Usage tracking table
CREATE TABLE usage_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id),
  agent_type TEXT NOT NULL,
  mode TEXT NOT NULL,
  
  -- Cost tracking
  tokens_input INTEGER DEFAULT 0,
  tokens_output INTEGER DEFAULT 0,
  estimated_cost DECIMAL(10,6) DEFAULT 0,
  model_used TEXT,
  
  -- Session data
  started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  ended_at TIMESTAMP WITH TIME ZONE,
  duration_seconds INTEGER,
  
  -- Performance metrics
  avg_response_time_ms INTEGER,
  error_count INTEGER DEFAULT 0,
  
  -- User feedback
  user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
  feedback TEXT,
  
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Daily usage limits tracking
CREATE TABLE daily_usage_limits (
  user_id UUID REFERENCES auth.users(id),
  date DATE DEFAULT CURRENT_DATE,
  
  text_sessions INTEGER DEFAULT 0,
  voice_sessions INTEGER DEFAULT 0,
  video_sessions INTEGER DEFAULT 0,
  images_generated INTEGER DEFAULT 0,
  
  total_cost DECIMAL(10,6) DEFAULT 0,
  
  PRIMARY KEY (user_id, date)
);
```

## Implementation Priorities

### Phase 1: Core Text + Image Mode
1. Implement smart model routing (Gemma → Flash-Lite)
2. Set up basic rate limiting
3. Create agent system prompts
4. Implement cost tracking

### Phase 2: Voice Integration
1. Add voice mode with fallback strategies
2. Implement session duration limits
3. Add graceful degradation from video → voice

### Phase 3: Live Video Guidance
1. Implement Live API integration
2. Real-time visual analysis pipeline
3. Premium feature gating
4. Advanced error handling

### Phase 4: Optimization
1. Advanced caching strategies
2. Predictive model selection
3. User behavior-based optimization
4. Premium conversion funnels

---

**Remember**: Quality over cost - use the best model for the task while staying within budget constraints. The goal is to create the world's best real-time AI guidance experience.