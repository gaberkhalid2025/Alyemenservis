#!/bin/bash
awk '
/fun acceptRequestOffer\(/ {
    print "    fun acceptRequestOffer("
    print "        req: com.example.data.models.InstantRequestEntity,"
    print "        offer: com.example.data.models.RequestOfferEntity"
    print "    ) {"
    print "        repository.acceptOffer("
    print "            request = req,"
    print "            offer = offer,"
    print "            onSuccess = { booking ->"
    print "                triggerNotification?.invoke(\"🎉 تم قبول عرض ${offer.technicianName} بنجاح وتحويل الطلب إلى حجز مؤكد!\")"
    print "                addNotification?.invoke("
    print "                    \"🎉 تم اختيار عرضك للطلب ${req.requestCode}\","
    print "                    \"تهانينا ${offer.technicianName}! اختار العميل ${req.userName} عرضك بسعر ${offer.price} ر.ي للطلب ${req.requestCode}. يمكنك البدء في التواصل والمباشرة الآن.\","
    print "                    \"PROVIDER\","
    print "                    offer.technicianPhone"
    print "                )"
    print "                val otherOffers = _requestOffers.value.filter { it.requestId == req.id && it.id != offer.id }"
    print "                otherOffers.forEach { otherOffer ->"
    print "                    addNotification?.invoke("
    print "                        \"📢 تم اختيار عرض آخر للطلب ${req.requestCode}\","
    print "                        \"شكراً لمشاركتك. تم اختيار عرض أسعار آخر من قبل العميل للطلب ${req.requestCode}.\","
    print "                        \"PROVIDER\","
    print "                        otherOffer.technicianPhone"
    print "                    )"
    print "                }"
    print "                getOrCreateChatChannel?.invoke(offer.technicianId, offer.technicianName, req.userPhone, req.userName)"
    print "            },"
    print "            onError = { err ->"
    print "                triggerNotification?.invoke(\"❌ خطأ: $err\")"
    print "            }"
    print "        )"
    print "    }"
    
    in_function = 1
    next
}

in_function {
    if (/^    fun completeInstantRequest\(/) {
        in_function = 0
        print $0
    }
    next
}

{ print $0 }
' app/src/main/java/com/example/ui/viewmodels/InstantRequestViewModel.kt > temp.kt && mv temp.kt app/src/main/java/com/example/ui/viewmodels/InstantRequestViewModel.kt
