# Recipe Parsing Test

## Problem
User received a recipe from Gemini but InteractiveCookingOverlay isn't showing up.

## Test Recipe (from logs)
```
# One-Pan Lemon Herb Roasted Chicken and Veggies

## Ingredients:

- 1.5 lbs bone-in, skin-on chicken thighs (or breasts, if preferred)
- 1 lb baby potatoes, halved or quartered if large
- 1 lb carrots, peeled and chopped into 1-inch pieces
- 1 red onion, cut into wedges
- 2 lemons, one thinly sliced, one juiced
- 2 tbsp olive oil
- 1 tbsp dried herbs (Italian mix, Herbes de Provence, or a custom blend of rosemary, thyme, and oregano work well)
- Salt and pepper to taste

## Instructions:

1. **Preheat your oven to 400°F (200°C).** [Tip:  Make sure your oven rack is in the middle position for even cooking.]

2. **Prepare the vegetables:** Wash and chop all the vegetables according to the ingredient list.

3. **Prepare the chicken:** Pat the chicken thighs dry with paper towels. This helps the skin crisp up nicely.

4. **Combine everything:** In a large bowl, toss the potatoes, carrots, and red onion with 1 tbsp of olive oil, salt, and pepper.  Place the vegetables in a single layer in a large roasting pan or baking sheet.

5. **Season and arrange the chicken:** Place the chicken thighs on top of the vegetables. Drizzle with the remaining olive oil and sprinkle with the dried herbs, salt, and pepper. Tuck the lemon slices under and around the chicken.

6. **Roast:** Roast for 45-60 minutes, or until the chicken is cooked through (internal temperature reaches 165°F/74°C) and the vegetables are tender.  [Tip:  Use a meat thermometer to ensure the chicken is cooked properly.  Cooking time may vary depending on the size and thickness of the chicken pieces.]

7. **Rest and serve:** Once cooked, remove the pan from the oven and let the chicken rest for 5-10 minutes before serving.  Squeeze the juice of the remaining lemon over the chicken and vegetables before serving.

[Image: A delicious one-pan roasted chicken and vegetables dish, with golden-brown chicken and tender vegetables]
```

## Expected Behavior
1. Recipe should be parsed successfully by `RecipeParser.parseRecipeFromText()`
2. "Start Interactive Cooking!" button should appear in chat
3. Clicking button should show `InteractiveCookingOverlay`

## Fixes Applied
1. ✅ **Fixed ArtifactDao interfaces** - Were commented out, causing artifact system to fail
2. ✅ **Fixed recipe parsing** - Added support for dash (-) bullet points
3. ✅ **Added debug logging** - To track parsing issues
4. ✅ **Removed cookingSessionManager null check** - Was blocking recipe detection

## Debug Information
Check logs for:
- `RecipeParser: Recipe parsing debug`
- `RecipeParser: Entered ingredients section`
- `RecipeParser: Found ingredient line`
- `RecipeParser: Entered instructions section`
- `RecipeParser: Found step line`

## Next Steps
1. Run the app
2. Ask cooking agent for a recipe
3. Check logs for parsing debug info
4. Look for "Start Interactive Cooking!" button
5. Test InteractiveCookingOverlay