with open("app/src/main/java/com/example/ui/viewmodels/BaseViewModel.kt", "r") as f:
    content = f.read()

extension_funcs = """
    open fun com.google.firebase.firestore.Query.addSnapshotListenerReg(listener: (com.google.firebase.firestore.QuerySnapshot?, com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit) {
        reg(this.addSnapshotListener(listener))
    }
    open fun com.google.firebase.firestore.DocumentReference.addSnapshotListenerReg(listener: (com.google.firebase.firestore.DocumentSnapshot?, com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit) {
        reg(this.addSnapshotListener(listener))
    }
"""

if "addSnapshotListenerReg" not in content:
    content = content.replace("open fun reg(", extension_funcs + "\n    open fun reg(")

with open("app/src/main/java/com/example/ui/viewmodels/BaseViewModel.kt", "w") as f:
    f.write(content)
