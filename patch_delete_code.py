import sys

def replace_in_file(filename, old_str, new_str):
    with open(filename, 'r') as f:
        content = f.read()
    
    if old_str in content:
        content = content.replace(old_str, new_str)
        with open(filename, 'w') as f:
            f.write(content)
        print(f"Patched {filename}")
    else:
        print(f"Not found in {filename}")

old_code = """                    val deleteCode = remember(order.id) { 
                        (order.id.hashCode().coerceAtLeast(0) % 9000 + 1000).toString() 
                    }"""

new_code = """                    val deleteCode = remember(order.id) { 
                        (1000..9999).random().toString() 
                    }"""

replace_in_file('app/src/main/java/com/example/ui/screens/bookings/OrdersScreenLayout.kt', old_code, new_code)
