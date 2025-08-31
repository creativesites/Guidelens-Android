package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.craftflowtechnologies.guidelens.R


@Composable
fun AgentSelectorDialog(
    agents: List<Agent>,
    selectedAgent: Agent,
    onAgentSelected: (Agent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Choose Your Assistant",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                agents.forEach { agent ->
                    AgentCard(
                        agent = agent,
                        isSelected = agent.id == selectedAgent.id,
                        onClick = { onAgentSelected(agent) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun AgentCard(
    agent: Agent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            agent.primaryColor.copy(alpha = 0.2f)
        } else {
            Color.White.copy(alpha = 0.1f)
        },
        border = if (isSelected) {
            BorderStroke(2.dp, agent.primaryColor)
        } else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (agent.id == "cooking"){
                Image(
                    painter = painterResource(id = R.drawable.cooking_agent_icon),
                    contentDescription = agent.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(agent.primaryColor.copy(0.3f), agent.secondaryColor.copy(0.3f))
                            )
                        ),
                )
            }
            if (agent.id == "crafting"){
                Image(
                    painter = painterResource(id = R.drawable.crafting_agent),
                    contentDescription = agent.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(agent.primaryColor.copy(0.3f), agent.secondaryColor.copy(0.3f))
                            )
                        ),
                )
            }
            if (agent.id == "companion"){
                Image(
                    painter = painterResource(id = R.drawable.companion_agent),
                    contentDescription = agent.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(agent.primaryColor.copy(0.3f), agent.secondaryColor.copy(0.3f))
                            )
                        ),
                )
            }
            if (agent.id == "diy"){
                Image(
                    painter = painterResource(id = R.drawable.diy_agent),
                    contentDescription = agent.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(agent.primaryColor.copy(0.3f), agent.secondaryColor.copy(0.3f))
                            )
                        ),

                )
            }
//            Box(
//                modifier = Modifier
//                    .size(48.dp)
//                    .clip(CircleShape)
//                    .background(
//                        brush = Brush.linearGradient(
//                            colors = listOf(agent.primaryColor, agent.secondaryColor)
//                        )
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = agent.icon,
//                    contentDescription = agent.name,
//                    tint = Color.White,
//                    modifier = Modifier.size(24.dp)
//                )
//
//
//            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = agent.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = agent.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
