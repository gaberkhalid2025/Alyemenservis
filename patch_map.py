import sys

def replace_between(filename, start_marker, end_marker, replacement):
    with open(filename, 'r') as f:
        content = f.read()
    
    start_idx = content.find(start_marker)
    if start_idx == -1:
        print("Start marker not found")
        return
        
    end_idx = content.find(end_marker, start_idx)
    if end_idx == -1:
        print("End marker not found")
        return
        
    end_idx += len(end_marker)
    
    new_content = content[:start_idx] + replacement + content[end_idx:]
    
    with open(filename, 'w') as f:
        f.write(new_content)
    print("Success")

replacement = """            // Navigation button
            if (hasGps) {
                IconButton(
                    onClick = {
                        val provider = settingsState.mapProvider
                        try {
                            when (provider) {
                                "MAPBOX" -> {
                                    val uri = android.net.Uri.parse("mapbox://directions/profile/mapbox/driving/$lng,$lat")
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    android.widget.Toast.makeText(context, "تم فتح Mapbox", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                "MAPLIBRE" -> {
                                    val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    android.widget.Toast.makeText(context, "تم فتح MapLibre / الخريطة الافتراضية", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    context.startActivity(intent)
                                    android.widget.Toast.makeText(context, "تم فتح خرائط جوجل", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            val fallbackUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri))
                        }
                    },
                    modifier = androidx.compose.ui.Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "توجيه", tint = Color(0xFF00E5FF))
                }
            }"""

replace_between('app/src/main/java/com/example/ui/screens/admin/AdminMapPanel.kt',
                '            // Navigation button\n            if (hasGps) {',
                '                }\n            }',
                replacement)
