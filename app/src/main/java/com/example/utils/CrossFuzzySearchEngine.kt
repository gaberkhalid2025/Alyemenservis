package com.example.utils

import com.example.utils.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Levenshtein distance fuzzy matching engine for search queries across categories.
 */
object LevenshteinMatcher {

    /** مسافة تحرير Damerau-Levenshtein مع دعم تبديل الحروف المتجاورة (Transposition) لتصحيح الأخطاء المطبعية العربية. */
    private fun rawDistance(s: String, t: String): Int {
        if (s == t) return 0
        if (s.isEmpty()) return t.length
        if (t.isEmpty()) return s.length
        val d = Array(s.length + 1) { IntArray(t.length + 1) }
        for (i in 0..s.length) d[i][0] = i
        for (j in 0..t.length) d[0][j] = j

        for (i in 1..s.length) {
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                var minVal = minOf(
                    d[i - 1][j] + 1,       // حذف
                    d[i][j - 1] + 1,       // إدراج
                    d[i - 1][j - 1] + cost // استبدال
                )
                if (i > 1 && j > 1 && s[i - 1] == t[j - 2] && s[i - 2] == t[j - 1]) {
                    minVal = minOf(minVal, d[i - 2][j - 2] + 1) // تبديل موضعي
                }
                d[i][j] = minVal
            }
        }
        return d[s.length][t.length]
    }

    fun calculateDistance(a: String, b: String): Int =
        rawDistance(ArabicNormalizer.normalize(a), ArabicNormalizer.normalize(b))

    /** درجة التطابق (0 الأفضل) أو null عند عدم التطابق. كل كلمات الاستعلام يجب أن تتطابق (AND). */
    fun score(query: String, target: String): Int? {
        val q = ArabicNormalizer.normalize(query)
        if (q.isEmpty()) return 0
        val t = ArabicNormalizer.normalize(target)
        if (t.isEmpty()) return null
        if (t == q) return 0
        if (t.startsWith(q)) return 1
        if (t.contains(q)) return 2
        if (t.length > 100) return null
        val qWords = q.split(" ").filter { it.isNotEmpty() }
        val tWords = t.split(" ").filter { it.isNotEmpty() }
        var total = 0
        for (qw in qWords) {
            val allowed = when { qw.length <= 3 -> 0; qw.length <= 5 -> 1; else -> 2 }
            val best = tWords.minOfOrNull { tw ->
                if (kotlin.math.abs(tw.length - qw.length) > allowed) Int.MAX_VALUE else rawDistance(qw, tw)
            } ?: Int.MAX_VALUE
            if (best > allowed) return null
            total += best
        }
        return 3 + total
    }

    fun isFuzzyMatch(query: String, target: String, maxDistance: Int = 3): Boolean = score(query, target) != null

    /** تقييم مطابقات اللهجة الموسعة واختيار أفضل درجة */
    fun scoreDialect(query: String, target: String): Int? =
        DialectSynonyms.expand(query).mapNotNull { score(it, target) }.minOrNull()

    fun isFuzzyMatchDialect(query: String, target: String): Boolean = scoreDialect(query, target) != null
}

data class SearchTechnicianItem(
    val id: String,
    val name: String,
    val profession: String,
    val rating: Double,
    val phone: String
)

data class SearchStoreItem(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val isVerified: Boolean = true
)

data class SearchArticleItem(
    val id: String,
    val title: String,
    val summary: String,
    val category: String
)

data class SearchResultData(
    val technicians: List<SearchTechnicianItem> = emptyList(),
    val stores: List<SearchStoreItem> = emptyList(),
    val articles: List<SearchArticleItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossFuzzySearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    allTechnicians: List<SearchTechnicianItem>,
    allStores: List<SearchStoreItem>,
    allArticles: List<SearchArticleItem>,
    onSelectTechnician: (SearchTechnicianItem) -> Unit,
    onSelectStore: (SearchStoreItem) -> Unit,
    onSelectArticle: (SearchArticleItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("👷 فنيون", "🏬 محلات وقوائم", "💡 مقالات وحلول")

    val filteredTechnicians = remember(query, allTechnicians) {
        if (query.isBlank()) allTechnicians else {
            allTechnicians.filter {
                LevenshteinMatcher.isFuzzyMatchDialect(query, it.name) ||
                        LevenshteinMatcher.isFuzzyMatchDialect(query, it.profession)
            }
        }
    }

    val filteredStores = remember(query, allStores) {
        if (query.isBlank()) allStores else {
            allStores.filter {
                LevenshteinMatcher.isFuzzyMatchDialect(query, it.name) ||
                        LevenshteinMatcher.isFuzzyMatchDialect(query, it.category) ||
                        LevenshteinMatcher.isFuzzyMatchDialect(query, it.address)
            }
        }
    }

    val filteredArticles = remember(query, allArticles) {
        if (query.isBlank()) allArticles else {
            allArticles.filter {
                LevenshteinMatcher.isFuzzyMatchDialect(query, it.title) ||
                        LevenshteinMatcher.isFuzzyMatchDialect(query, it.summary) ||
                        LevenshteinMatcher.isFuzzyMatchDialect(query, it.category)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث متسامح بالأخطاء الإملائية (فنيون، محلات، مقالات)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        when (selectedTabIndex) {
            0 -> { // Technicians
                if (filteredTechnicians.isEmpty()) {
                    EmptyResultPlaceholder("لا توجد نتائج فنيين مطابقة للبحث")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredTechnicians) { tech ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTechnician(tech) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tech.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(tech.profession, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    }
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("★ ${tech.rating}", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> { // Stores
                if (filteredStores.isEmpty()) {
                    EmptyResultPlaceholder("لا توجد محلات أو مراكز مطابقة للبحث")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredStores) { store ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectStore(store) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(store.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${store.category} - ${store.address}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> { // Articles
                if (filteredArticles.isEmpty()) {
                    EmptyResultPlaceholder("لا توجد مقالات أو حلول مطابقة")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredArticles) { article ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectArticle(article) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(article.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(article.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResultPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Gray, fontSize = 14.sp)
    }
}
