with open("app/src/main/java/com/example/ui/components/FloatingIconsOverlay.kt", "r") as f:
    content = f.read()

# Remove the draggable code
drag_code = """                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }"""
content = content.replace(drag_code, "")
content = content.replace("                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }\n", "")
content = content.replace("        var offsetX by remember { mutableFloatStateOf(0f) }\n", "")
content = content.replace("        var offsetY by remember { mutableFloatStateOf(0f) }\n", "")

with open("app/src/main/java/com/example/ui/components/FloatingIconsOverlay.kt", "w") as f:
    f.write(content)
