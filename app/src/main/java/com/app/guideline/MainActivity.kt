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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : ComponentActivity() {

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
fun HomeScreen(supabase: SupabaseClient) {
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var selectedTehsil by remember { mutableStateOf<String?>(null) }
    var selectedSubArea by remember { mutableStateOf<String?>(null) }
    var selectedWard by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }

    var districts by remember { mutableStateOf<List<String>>(emptyList()) }
    var tehsils by remember { mutableStateOf<List<String>>(emptyList()) }
    var subAreas by remember { mutableStateOf<List<String>>(emptyList()) }
    var wards by remember { mutableStateOf<List<String>>(emptyList()) }
    var locations by remember { mutableStateOf<List<String>>(emptyList()) }

    var guidelineData by remember { mutableStateOf<JsonObject?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var activeSheetTitle by remember { mutableStateOf<String?>(null) }
    var activeSheetItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeSheetType by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun fetchTehsils(district: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = supabase.postgrest.rpc("get_tehsils", mapOf("p_district" to district)).decodeList<JsonObject>()
                val list = res.mapNotNull { it["tehsil"]?.jsonPrimitive?.content?.trim() }.filter { it.isNotEmpty() }
                withContext(Dispatchers.Main) {
                    tehsils = list
                    selectedTehsil = list.firstOrNull()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchSubAreas(district: String, tehsil: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = supabase.postgrest.rpc("get_sub_areas", mapOf("p_district" to district, "p_tehsil" to tehsil)).decodeList<JsonObject>()
                val list = res.mapNotNull { it["sub_area"]?.jsonPrimitive?.content?.trim() }.filter { it.isNotEmpty() }
                withContext(Dispatchers.Main) {
                    subAreas = list
                    selectedSubArea = list.firstOrNull()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchWards(district: String, tehsil: String, subArea: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = supabase.postgrest.rpc("get_wards", mapOf("p_district" to district, "p_tehsil" to tehsil, "p_sub_area" to subArea)).decodeList<JsonObject>()
                val list = res.mapNotNull { it["ward_halka"]?.jsonPrimitive?.content?.trim() }.filter { it.isNotEmpty() }
                withContext(Dispatchers.Main) {
                    wards = list
                    selectedWard = list.firstOrNull()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchLocations(district: String, tehsil: String, subArea: String, ward: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = supabase.postgrest.rpc("get_locations", mapOf("p_district" to district, "p_tehsil" to tehsil, "p_sub_area" to subArea, "p_ward" to ward)).decodeList<JsonObject>()
                val list = res.mapNotNull { it["location_name"]?.jsonPrimitive?.content?.trim() }.filter { it.isNotEmpty() }
                withContext(Dispatchers.Main) {
                    locations = list
                    selectedLocation = list.firstOrNull()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchRates(district: String, tehsil: String, subArea: String, ward: String, loc: String) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val res = supabase.postgrest.from("guidelines")
                    .select {
                        filter {
                            eq("district", district)
                            eq("tehsil", tehsil)
                            eq("sub_area", subArea)
                            eq("ward_halka", ward)
                            eq("location_name", loc)
                        }
                        limit(1)
                    }.decodeSingleOrNull<JsonObject>()
                withContext(Dispatchers.Main) {
                    guidelineData = res
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = supabase.postgrest.rpc("get_districts").decodeList<JsonObject>()
                val list = res.mapNotNull { it["district"]?.jsonPrimitive?.content?.trim() }.filter { it.isNotEmpty() }
                withContext(Dispatchers.Main) {
                    districts = list
                    val d = if (list.contains("मंदसौर")) "मंदसौर" else list.firstOrNull()
                    selectedDistrict = d
                    if (d != null) {
                        fetchTehsils(d)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कलेक्टर गाइडलाइन 2026-27", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E3A8A))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                            
                            SelectionTile("1", "ज़िला", selectedDistrict) {
                                activeSheetTitle = "ज़िला चुनें"
                                activeSheetItems = districts
                                activeSheetType = "district"
                            }

                            SelectionTile("2", "तहसील", selectedTehsil, isEnabled = selectedDistrict != null && tehsils.isNotEmpty()) {
                                activeSheetTitle = "तहसील चुनें"
                                activeSheetItems = tehsils
                                activeSheetType = "tehsil"
                            }

                            SelectionTile("3", "निकाय / उप-क्षेत्र", selectedSubArea, isEnabled = selectedTehsil != null && subAreas.isNotEmpty()) {
                                activeSheetTitle = "निकाय / उप-क्षेत्र चुनें"
                                activeSheetItems = subAreas
                                activeSheetType = "sub_area"
                            }

                            SelectionTile("4", "वार्ड / हल्का", selectedWard, isEnabled = selectedSubArea != null && wards.isNotEmpty()) {
                                activeSheetTitle = "वार्ड / हल्का चुनें"
                                activeSheetItems = wards
                                activeSheetType = "ward"
                            }

                            SelectionTile("5", "कॉलोनी / गाँव", selectedLocation, isEnabled = selectedWard != null && locations.isNotEmpty()) {
                                activeSheetTitle = "कॉलोनी / गाँव चुनें"
                                activeSheetItems = locations
                                activeSheetType = "location"
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (guidelineData != null) {
                    item {
                        RateDetailsCard(guidelineData!!)
                    }
                }
            }

            if (activeSheetTitle != null) {
                SelectionBottomSheet(
                    title = activeSheetTitle!,
                    items = activeSheetItems,
                    onDismiss = { activeSheetTitle = null },
                    onSelect = { v ->
                        activeSheetTitle = null
                        when (activeSheetType) {
                            "district" -> {
                                selectedDistrict = v
                                selectedTehsil = null
                                selectedSubArea = null
                                selectedWard = null
                                selectedLocation = null
                                guidelineData = null
                                fetchTehsils(v)
                            }
                            "tehsil" -> {
                                selectedTehsil = v
                                selectedSubArea = null
                                selectedWard = null
                                selectedLocation = null
                                guidelineData = null
                                selectedDistrict?.let { fetchSubAreas(it, v) }
                            }
                            "sub_area" -> {
                                selectedSubArea = v
                                selectedWard = null
                                selectedLocation = null
                                guidelineData = null
                                val dist = selectedDistrict
                                val teh = selectedTehsil
                                if (dist != null && teh != null) fetchWards(dist, teh, v)
                            }
                            "ward" -> {
                                selectedWard = v
                                selectedLocation = null
                                guidelineData = null
                                val dist = selectedDistrict
                                val teh = selectedTehsil
                                val sub = selectedSubArea
                                if (dist != null && teh != null && sub != null) fetchLocations(dist, teh, sub, v)
                            }
                            "location" -> {
                                selectedLocation = v
                                val dist = selectedDistrict
                                val teh = selectedTehsil
                                val sub = selectedSubArea
                                val ward = selectedWard
                                if (dist != null && teh != null && sub != null && ward != null) fetchRates(dist, teh, sub, ward, v)
                            }
                        }
                    }
                )
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
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
                Text(value ?: "चुनें...", fontSize = 14.sp, fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal, color = if (value != null) Color(0xFF0F172A) else Color.Gray)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isEnabled) Color(0xFF1E3A8A) else Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionBottomSheet(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = items.filter { it.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("यहाँ खोजें...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredItems) { item ->
                    Card(
                        onClick = { onSelect(item) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RateDetailsCard(d: JsonObject) {
    val locName = d["location_name"]?.jsonPrimitive?.content ?: ""
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(locName, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A)))
            Spacer(modifier = Modifier.height(10.dp))

            RateSection("1. भूखण्ड दरें (Plot Rates ₹/वर्ग मी.)", listOf(
                Pair("आवासीय भूखण्ड", d["plot_residential"]?.jsonPrimitive?.content),
                Pair("व्यावसायिक भूखण्ड", d["plot_commercial"]?.jsonPrimitive?.content),
                Pair("औद्योगिक भूखण्ड", d["plot_industrial"]?.jsonPrimitive?.content)
            ))

            RateSection("2. आवासीय निर्माण (₹/वर्ग मी.)", listOf(
                Pair("RCC निर्माण", d["rcc_residential"]?.jsonPrimitive?.content),
                Pair("पक्का निर्माण", d["pucca_residential"]?.jsonPrimitive?.content),
                Pair("अर्ध-पक्का निर्माण", d["semi_pucca_residential"]?.jsonPrimitive?.content),
                Pair("कच्चा / टीन शेड", d["kachha_residential"]?.jsonPrimitive?.content)
            ))

            RateSection("3. दुकान / व्यावसायिक (₹/वर्ग मी.)", listOf(
                Pair("दुकान (RCC)", d["shop_rcc"]?.jsonPrimitive?.content),
                Pair("दुकान (पक्का)", d["shop_pucca"]?.jsonPrimitive?.content),
                Pair("दुकान (अर्ध-पक्का)", d["shop_semi_pucca"]?.jsonPrimitive?.content)
            ))

            RateSection("4. कृषि भूमि (₹/हेक्टेयर)", listOf(
                Pair("🌾 सिंचित भूमि", d["agri_irrigated"]?.jsonPrimitive?.content),
                Pair("🍂 असिंचित भूमि", d["agri_unirrigated"]?.jsonPrimitive?.content)
            ))
        }
    }
}

@Composable
fun RateSection(title: String, rates: List<Pair<String, String?>>) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
        Spacer(modifier = Modifier.height(4.dp))
        rates.forEach { (label, value) ->
            val displayVal = if (value.isNullOrEmpty() || value == "null" || value == "None") "-" else "₹ $value"
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 12.5.sp, color = Color(0xFF475569))
                Text(displayVal, fontSize = 13.sp, fontWeight = FontWeight.W600, color = Color(0xFF0F172A))
            }
        }
    }
}
