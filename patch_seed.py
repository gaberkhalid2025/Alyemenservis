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

replacement = """    fun writeDefaultSupervisors() {
        /*
         * تم إزالة آلية التشفير بـ XOR وتم نقل كافة بيانات تسجيل الدخول الافتراضية والسرية إلى
         * Firebase Cloud Functions (مثل initializeAdminCredentials).
         * الغرض: تأمين الكود المصدري من أي تسريب لكلمات المرور أو الأبواب الخلفية.
         * يجب عدم إعادة تخزين كلمات مرور صريحة هنا مستقبلاً.
         */
    }"""

replace_between('app/src/main/java/com/example/ui/helpers/FirestoreSeedHelper.kt',
                '    fun writeDefaultSupervisors() {',
                '        }',
                replacement)
