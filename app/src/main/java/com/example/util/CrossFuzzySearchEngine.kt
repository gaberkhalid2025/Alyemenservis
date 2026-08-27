package com.example.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Offer
import com.example.data.ProductEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import java.util.Locale
import kotlin.math.min

/**
 * 🔍 CrossFuzzySearchEngine - محرك البحث الضبابي المتسامح بالأخطاء الإملائية
 * 
 * الميزات:
 * 1. خوارزمية Levenshtein Distance لحساب المسافة التحريرية والتسامح مع الأخطاء الطباعية.
 * 2. دمج خوارزمية Soundex للبحث الصوتي عبر `SearchEnhancer`.
 * 3. دمج البحث بالمرادفات واللهجات اليمنية لربط الكلمات الدارجة بالفئات الفنية.
 * 4. البحث السياقي (Contextual Search) المتكامل عبر الفنيين والمتاجر والحلول.
 */
object CrossFuzzySearchEngine {

    /**
     * حساب مسافة Levenshtein بين كلمتين
     * @param s1 النص الأول
     * @param s2 النص الثاني
     * @return عدد العمليات اللازمة للتحويل
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> {
                        val cost = if (s1[i - 1].lowercaseChar() == s2[j - 1].lowercaseChar()) 0 else 1
                        dp[i][j] = min(
                            dp[i - 1][j] + 1,      // حذف
                            min(
                                dp[i][j - 1] + 1,  // إضافة
                                dp[i - 1][j - 1] + cost // استبدال
                            )
                        )
                    }
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * حساب نسبة التشابه بين نصين (من 0.0 إلى 1.0)
     */
    fun similarityRatio(s1: String, s2: String): Double {
        if (s1.equals(s2, ignoreCase = true)) return 1.0
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1, s2)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    /**
     * فحص مطابقة البحث المتقدم (Levenshtein + Soundex + المرادفات + السياق)
     * 
     * @param query نص البحث
     * @param target النص المستهدف
     * @param threshold حد القبول للتشابه (الافتراضي 0.6)
     * @return true إذا كانت النتيجة متطابقة أو مقبولة
     */
    fun isFuzzyMatch(query: String, target: String, threshold: Double = 0.6): Boolean {
        if (query.isBlank() || target.isBlank()) return false
        val cleanQuery = SearchEnhancer.normalizeArabicPhonetics(query.trim())
        val cleanTarget = SearchEnhancer.normalizeArabicPhonetics(target.trim())

        // 1. التطابق المباشر
        if (cleanTarget.contains(cleanQuery)) return true

        // 2. البحث بالسياق والمرادفات والصوتيات
        val contextualScore = SearchEnhancer.calculateContextualScore(cleanQuery, cleanTarget)
        if (contextualScore >= threshold) return true

        // 3. فحص نسبة Levenshtein للكلمات
        val targetWords = cleanTarget.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val queryWords = cleanQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }

        for (q in queryWords) {
            for (t in targetWords) {
                if (similarityRatio(q, t) >= threshold) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * تصفية الفنيين باستخدام البحث الضبابي والسياقي
     */
    fun filterTechnicians(query: String, list: List<TechnicianSearchItem>): List<TechnicianSearchItem> {
        if (query.isBlank()) return list
        return list.filter { item ->
            isFuzzyMatch(query, item.name) ||
            isFuzzyMatch(query, item.profession) ||
            item.keywords.any { isFuzzyMatch(query, it) }
        }
    }

    /**
     * تصفية المتاجر والمراكز التجارية
     */
    fun filterStores(query: String, list: List<StoreSearchItem>): List<StoreSearchItem> {
        if (query.isBlank()) return list
        return list.filter { item ->
            isFuzzyMatch(query, item.name) ||
            isFuzzyMatch(query, item.category) ||
            isFuzzyMatch(query, item.address) ||
            item.keywords.any { isFuzzyMatch(query, it) }
        }
    }

    /**
     * تصفية المقالات والحلول الفنية
     */
    fun filterArticles(query: String, list: List<ArticleSearchItem>): List<ArticleSearchItem> {
        if (query.isBlank()) return list
        return list.filter { item ->
            isFuzzyMatch(query, item.title) ||
            isFuzzyMatch(query, item.summary) ||
            item.tags.any { isFuzzyMatch(query, it) }
        }
    }
}

// ==========================================
// نماذج البحث الموحدة
// ==========================================

data class TechnicianSearchItem(
    val id: String,
    val name: String,
    val profession: String,
    val rating: Double,
    val keywords: List<String> = emptyList()
)

data class StoreSearchItem(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val keywords: List<String> = emptyList()
)

data class ArticleSearchItem(
    val id: String,
    val title: String,
    val summary: String,
    val tags: List<String> = emptyList()
)

// ==========================================
// واجهة البحث المتسامح بالأخطاء الإملائية
// ==========================================

@Composable
fun CrossFuzzySearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    technicians: List<TechnicianSearchItem>,
    stores: List<StoreSearchItem>,
    articles: List<ArticleSearchItem>,
    onSelectTechnician: (TechnicianSearchItem) -> Unit,
    onSelectStore: (StoreSearchItem) -> Unit,
    onSelectArticle: (ArticleSearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("فنيون 👨‍🔧", "محلات ومراكز 🏪", "مقالات وإرشادات 📖")

    val filteredTechnicians = remember(query, technicians) {
        CrossFuzzySearchEngine.filterTechnicians(query, technicians)
    }
    val filteredStores = remember(query, stores) {
        CrossFuzzySearchEngine.filterStores(query, stores)
    }
    val filteredArticles = remember(query, articles) {
        CrossFuzzySearchEngine.filterArticles(query, articles)
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
            placeholder = { Text("بحث ذكي متسامح بالأخطاء (فنيون، محلات، حلول)...") },
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
