package com.app.guideline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.MobileAds
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : ComponentActivity() {

    // Supabase Client Initialization
    val supabase = createSupabaseClient(
        supabaseUrl = "https://dcbtdftgjyokioqcpumv.supabase.co",
        supabaseKey = "sb_publishable_u6nxcL145Z-HsaKiOlfjoQ_DCvKJrGO"
    ) {
        install(Postgrest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF1F5F9)
            ) {
                HomeScreen(supabase)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(supabase: io.github.jan.supabase.SupabaseClient) {
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var districts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load Districts on Startup from Supabase
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val result = supabase.postgrest.rpc("get_districts").decodeList<JsonObject>()
                val list = result.mapNotNull { it["district"]?.jsonPrimitive?.content?.trim() }
                    .filter { it.isNotEmpty() }
                
                withContext(Dispatchers.Main) {
                    districts = list
                    isLoading = false
                    if (districts.isNotEmpty()) {
                        selectedDistrict = districts.contains("मंदसौर") ? "मंदसौर" : districts.first()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = e.localizedMessage ?: "Connection Error"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "कलेक्टर गाइडलाइन 2026-27", 
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E3A8A))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("डेटा लोड करने में त्रुटि:", color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!, color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📍 स्थान चुनें", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A)))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SelectionTile("1", "ज़िला", selectedDistrict) {}
                            }
                        }
                    }

                    item {
                        Text("उपलब्ध ज़िले (Database से):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    }

                    items(districts) { district ->
                        Card(
                            onClick = { selectedDistrict = district },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDistrict == district) Color(0xFF1E3A8A) else Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = district,
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedDistrict == district) Color.White else Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionTile(step: String, label: String, value: String?, isEnabled: Boolean = true, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isEnabled) Color(0xFFF8FAFC) else Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(if (isEnabled) Color(0xFF1E3A8A) else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = Color.White, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.dp)) {
                Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
                Text(value ?: "चुनें...", fontSize = 14.sp, fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal, color = if (value != null) Color(0xFF0F172A) else Color.Gray)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isEnabled) Color(0xFF1E3A8A) else Color.Gray)
        }
    }
}
