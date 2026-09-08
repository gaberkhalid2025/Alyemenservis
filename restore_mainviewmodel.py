import re

content = """
493: fun applyFilters() {
494:         homeViewModel.applyFilters(_currentUserResidence.value)
495:     }
496: fun selectCategory(categoryId: String?) {
497:         homeViewModel.selectCategory(categoryId, _currentUserResidence.value)
498:     }
499: fun updateSearchQuery(query: String) {
500:         homeViewModel.updateSearchQuery(query, _currentUserResidence.value)
501:     }
502: fun toggleVipFilter() {
503:         homeViewModel.toggleVipFilter(_currentUserResidence.value)
504:     }
505: fun toggleAvailableFilter() {
506:         homeViewModel.toggleAvailableFilter(_currentUserResidence.value)
507:     }
508: fun setCityFilter(cityId: String?) {
509:         homeViewModel.setCityFilter(cityId, _currentUserResidence.value)
510:     }
511: fun setNeighborhoodFilter(neighborhood: String) {
512:         homeViewModel.setNeighborhoodFilter(neighborhood, _currentUserResidence.value)
513:     }
514: fun setPhoneOrNameFilter(text: String) {
515:         homeViewModel.setPhoneOrNameFilter(text, _currentUserResidence.value)
516:     }
517: fun setRadiusKm(km: Int) {
518:         homeViewModel.setRadiusKm(km, _currentUserResidence.value)
519:     }
520: fun registerBackdoorInteraction() {
521:         authViewModel.registerBackdoorInteraction()
522:     }
523: fun changeAdminCredentials(username: String, password: String) {
524:         triggerNotification("🔐 تم تغيير بيانات المدير الرئيسي")
525:     }
526:     fun authenticateAdmin(context: android.content.Context, role: String, remember: Boolean) {
527:         authViewModel.authenticateAdmin(context, role, remember)
528:         navigateTo("ADMIN_PANEL")
529:     }
530:     fun authenticateAdmin(role: String) {
531:         authViewModel.authenticateAdmin(role)
532:         navigateTo("ADMIN_PANEL")
533:     }
534:     fun logout(context: android.content.Context) {
535:         authViewModel.logout(context)
536:         navigateTo("USER_BROWSE")
537:     }
538:     fun navigateToScreen(screen: String) = navigateTo(screen)
539:     fun navigateAndRemoveScreens(targetScreen: String, screensToRemove: List<String>) {
540:         val updated = _screenBackStack.value.toMutableList()
541:         updated.removeAll(screensToRemove.toSet())
542:         if (targetScreen == "USER_BROWSE") {
543:             updated.clear()
544:             updated.add("USER_BROWSE")
545:         } else if (!updated.contains(targetScreen)) {
546:             updated.add(targetScreen)
547:         }
548:         _screenBackStack.value = updated
549:         _currentScreen.value = targetScreen
550:     }
551:     fun navigateTo(screen: String) {
552:         if (_currentScreen.value != screen) {
553:             val updated = _screenBackStack.value.toMutableList()
554:             if (screen == "USER_BROWSE") {
555:                 updated.clear()
556:                 updated.add("USER_BROWSE")
557:             } else {
558:                 updated.add(screen)
559:             }
560:             _screenBackStack.value = updated
561:             _currentScreen.value = screen
562:         }
563:     }
564: fun goBack(): Boolean {
565:         val stack = _screenBackStack.value.toMutableList()
566:         if (stack.size > 1) {
567:             stack.removeAt(stack.size - 1)
568:             val prev = stack.last()
569:             _screenBackStack.value = stack
570:             _currentScreen.value = prev
571:             return true
572:         } else if (_currentScreen.value != "USER_BROWSE") {
573:             _screenBackStack.value = listOf("USER_BROWSE")
574:             _currentScreen.value = "USER_BROWSE"
575:             return true
576:         }
577:         return false
578:     }
579: fun switchLanguage() {
580:         val ctx = appContext
581:         if (ctx != null) {
582:             val newLang = LocaleManager.toggleLanguage(ctx)
583:             _currentLanguage.value = newLang
584:         } else {
585:             val newLang = if (_currentLanguage.value == "ar") "en" else "ar"
586:             _currentLanguage.value = newLang
587:         }
588:     }
589: fun toggleLanguage(context: android.content.Context) {
590:         appContext = context.applicationContext
591:         val newLang = LocaleManager.toggleLanguage(context)
592:         _currentLanguage.value = newLang
593:     }
594: fun setLanguage(lang: String) {
595:         _currentLanguage.value = lang
596:         val ctx = appContext
597:         if (ctx != null) {
598:             LocaleManager.setLanguage(ctx, lang)
599:         }
600:         val newSettings = _settings.value.copy(appLanguage = lang)
601:         _settings.value = newSettings
602:         try {
603:             db.collection("settings").document("main_settings").update("appLanguage", lang)
604:         } catch (e: Exception) {
605:             android.util.Log.e("MainViewModel", "Error: ", e)
606:         }
607:     }
608: fun setLanguage(context: android.content.Context, lang: String) {
609:         appContext = context.applicationContext
610:         setLanguage(lang)
611:     }
612: fun triggerNotification(
613:         title: String,
614:         message: String,
615:         targetType: String = "ALL",
616:         targetValue: String = "",
617:         context: android.content.Context? = null
618:     ) {
619:         val newNotif = com.example.data.NotificationEntity(
620:             id = java.util.UUID.randomUUID().toString(),
621:             title = title,
622:             message = message,
623:             targetType = targetType,
624:             targetValue = targetValue,
625:             timestamp = System.currentTimeMillis()
626:         )
627:         val currentList = _notifications.value.toMutableList()
628:         currentList.add(0, newNotif)
629:         _notifications.value = currentList
630:         try {
631:             db.collection("notifications").document(newNotif.id).set(newNotif)
632:         } catch (e: Exception) {
633:             android.util.Log.e("MainViewModel", "Error: ", e)
634:         }
635:         triggerNotification("$title: $message", context)
636:     }
637:     private var lastNotifMsg: String = ""
638:     private var lastNotifTime: Long = 0L
639:     fun triggerNotification(msg: String, context: android.content.Context? = null) {
640:         val now = System.currentTimeMillis()
641:         if (msg == lastNotifMsg && (now - lastNotifTime) < 3000L) {
642:             return
643:         }
644:         lastNotifMsg = msg
645:         lastNotifTime = now
646:         _toastMessage.value = msg
647:         val ctx = context ?: appContext
648:         if (ctx != null) {
649:             try {
650:                 val channelId = "yemen_services_alerts"
651:                 val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
652:                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
653:                     val channel = android.app.NotificationChannel(
654:                         channelId,
655:                         "إشعارات الخدمة والحجوزات والمحادثات",
656:                         android.app.NotificationManager.IMPORTANCE_HIGH
657:                     ).apply {
658:                         description = "إشعارات التطبيق الفورية"
659:                         enableVibration(true)
660:                     }
661:                     nm?.createNotificationChannel(channel)
662:                 }
663:                 val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
664:                     flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
665:                 }
666:                 val pendingIntent = android.app.PendingIntent.getActivity(
667:                     ctx, (System.currentTimeMillis() % 1000).toInt(), intent,
668:                     android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
669:                 )
670:                 val builder = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
671:                     .setSmallIcon(android.R.drawable.ic_dialog_info)
672:                     .setContentTitle("دليل خدمات اليمن 🔔")
673:                     .setContentText(msg)
674:                     .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(msg))
675:                     .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
676:                     .setAutoCancel(true)
677:                     .setContentIntent(pendingIntent)
678:                 nm?.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
679:             } catch (e: Exception) {
680:                 android.util.Log.e("MainViewModel", "Error: ", e)
681:             }
682:         }
683:     }
684: fun triggerOpenChatForRequest(requestId: String, customerPhone: String, serviceType: String) {
685:         val targetPhone = customerPhone.ifBlank { _currentUserPhone.value }
686:         if (targetPhone.isNotBlank()) {
687:             getOrCreateChatChannel(
688:                 providerId = "request_$requestId",
689:                 providerName = "صاحب الطلب ($targetPhone)",
690:                 customerId = _currentUserPhone.value.ifBlank { "guest" },
691:                 customerName = _currentUserName.value.ifBlank { "مستخدم الدليل" }
692:             )
693:         } else {
694:             triggerNotification("💬 يمكنك التحدث مع مقدمي العروض عبر شاشة المحادثات")
695:         }
696:     }
697: fun clearNotification() {
698:         _toastMessage.value = null
699:     }
700: fun loadUserPoints() {
701:         _currentUserPoints.value = (100..500).random()
702:     }
703: fun redeemLoyaltyPoints() {
704:         triggerNotification("🎉 تم استبدال نقاطك بنجاح! تم الخصم بنجاح.")
705:     }
706: fun rewardSharePoints() {
707:         _currentUserPoints.value = _currentUserPoints.value + 20
708:         triggerNotification("🎁 حصلت على 20 نقطة مشاركة!")
709:     }
710: fun clearSmartAssistantChatHistory() {
711:         _currentUserPoints.value = 0
712:         triggerNotification("🧹 تم تصفية وحذف سجل المحادثة الذكية بنجاح!")
713:     }
714:     fun submitJoinForm(
715:         context: android.content.Context,
716:         name: String, phone: String, catId: String, area: String,
717:         neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String,
718:         workPhotos: List<String> = emptyList(),
719:         customCategoryName: String = "",
720:         password: String = "",
721:         productAttachmentsJson: String = ""
722:     ) {
723:         registrationHelper.submitJoinForm(
724:             context = context,
725:             scope = viewModelScope,
726:             name = name,
727:             phone = phone,
728:             catId = catId,
729:             area = area,
730:             neighborhood = neighborhood,
731:             photoPath = photoPath,
732:             idCardPath = idCardPath,
733:             gpsCoords = gpsCoords,
734:             workPhotos = workPhotos,
735:             customCategoryName = customCategoryName,
736:             password = password,
737:             productAttachmentsJson = productAttachmentsJson,
738:             checkDuplicate = { checkAndGetDuplicateAccountType(it, "") },
739:             logAdminActivity = { logAdminActivity(it) },
740:             triggerNotification = { triggerNotification(it) },
741:             addApplicantNotification = { title, msg, type, targetVal ->
742:                 addNotification(
743:                     title = title,
744:                     message = msg,
745:                     targetType = type,
746:                     targetValue = targetVal
747:                 )
748:             },
749:             onPendingAdded = { newReq ->
750:                 val currentPending = _pendingProviders.value.filter { it.id != newReq.id }.toMutableList()
751:                 currentPending.add(newReq)
752:                 _pendingProviders.value = currentPending
753:                 val currentTechs = _pendingTechnicians.value.filter { it.id != newReq.id }.toMutableList()
754:                 currentTechs.add(newReq)
755:                 _pendingTechnicians.value = currentTechs
756:             },
757:             onStoreAdded = { newStore ->
758:                 val sList = _stores.value.toMutableList()
759:                 sList.removeAll { it.id == newStore.id }
760:                 sList.add(newStore)
761:                 _stores.value = sList
762:             },
763:             onPropertyAdded = { newProp ->
764:                 val pList = _properties.value.toMutableList()
765:                 pList.removeAll { it.id == newProp.id }
766:                 pList.add(newProp)
767:                 _properties.value = pList
768:             },
769:             onJobAdded = { newJob ->
770:                 val jList = _jobs.value.toMutableList()
771:                 jList.removeAll { it.id == newJob.id }
772:                 jList.add(newJob)
773:                 _jobs.value = jList
774:             },
775:             onClientAdded = { userMap ->
776:                 val uList = _registeredUsersList.value.toMutableList()
777:                 uList.removeAll { it["phone"] == userMap["phone"] }
778:                 uList.add(userMap)
779:                 _registeredUsersList.value = uList
780:             },
781:             onJoinRequestPhoneUpdated = { _joinRequestPhone.value = it },
782:             onNavigateToScreen = { _currentScreen.value = it }
783:         )
784:     }
785: 
786:     fun registerClientUser(name: String, phone: String, residence: String, password: String = "") {
787:         registrationHelper.registerClientUser(name, phone, residence, password) { userMap ->
788:             val uList = _registeredUsersList.value.toMutableList()
789:             uList.removeAll { it["phone"] == userMap["phone"] }
790:             uList.add(userMap)
791:             _registeredUsersList.value = uList
792:         }
793:     }
794: 
795:     fun cancelOrResetJoinRequest(context: android.content.Context) {
796:         registrationHelper.cancelOrResetJoinRequest(
797:             context = context,
798:             phone = _joinRequestPhone.value,
799:             pendingProviders = _pendingProviders.value,
800:             onPendingRemoved = { id ->
801:                 _pendingProviders.value = _pendingProviders.value.filter { it.id != id }
802:             },
803:             onPhoneCleared = { _joinRequestPhone.value = "" },
804:             onGoBack = { goBack() }
805:         )
806:     }
807: 
808:     fun setJoinRequestPhone(context: android.content.Context, phone: String) {
809:         registrationHelper.setJoinRequestPhone(context, phone) {
810:             _joinRequestPhone.value = it
811:         }
812:     }
813: 
814: fun addNotification(
815:         title: String,
816:         message: String,
817:         targetType: String = "ALL",
818:         targetValue: String = "",
819:         targetAudience: String = "ALL",
820:         targetRoles: List<String> = emptyList(),
821:         targetUserIds: List<String> = emptyList(),
822:         senderId: String = "SYSTEM",
823:         senderName: String = "النظام",
824:         dedupKey: String = "",
825:         expiryTimestamp: Long = 0L,
826:         scheduledTime: Long = 0L,
827:         customerPhone: String = "",
828:         customerName: String = "",
829:         notificationType: String = "NORMAL",
830:         channel: String = "IN_APP"
831:     ) {
832:         if (title.trim().isEmpty() || message.trim().isEmpty()) {
833:             return
834:         }
835:         val providerByPhone = _providers.value.find { it.phone.trim() == targetValue.trim() }
836:         val providerById = _providers.value.find { it.id == targetValue }
837:         val isNotifDisabled = (providerByPhone?.isNotificationsDisabled == true) || (providerById?.isNotificationsDisabled == true)
838:         if (isNotifDisabled) {
839:             triggerNotification("⚠️ تم حجب إرسال هذا الإشعار لأن الإدارة قامت بتعطيل إشعارات الفني: ${providerByPhone?.name ?: providerById?.name ?: ""}")
840:             return
841:         }
842:         val finalDedupKey = if (dedupKey.isNotBlank()) dedupKey else "${notificationType}_${targetValue}_${title}_${System.currentTimeMillis() / (30 * 1000L)}"
843:         val isDuplicate = _notifications.value.any { it.dedupKey == finalDedupKey || (it.title == title && it.targetValue == targetValue && Math.abs(it.timestamp - System.currentTimeMillis()) < 15000L) }
844:         if (isDuplicate) {
845:             return
846:         }
847:         val newNotif = NotificationEntity(
848:             id = "n_" + UUID.randomUUID().toString().take(8),
849:             title = title.trim(),
850:             message = message.trim(),
851:             targetType = targetType,
852:             targetValue = targetValue,
853:             targetAudience = targetAudience,
854:             targetRoles = targetRoles,
855:             targetUserIds = targetUserIds,
856:             senderId = senderId,
857:             senderName = senderName,
858:             dedupKey = finalDedupKey,
859:             timestamp = System.currentTimeMillis(),
860:             expiryTimestamp = expiryTimestamp,
861:             scheduledTime = scheduledTime,
862:             customerPhone = customerPhone,
863:             customerName = customerName,
864:             notificationType = notificationType,
865:             channel = channel
866:         )
867:         _notifications.value = listOf(newNotif) + _notifications.value.filter { it.id != newNotif.id }
868:         try {
869:             db.collection("notifications").document(newNotif.id).set(newNotif)
870:         } catch (e: Exception) {
871:             android.util.Log.e("MainViewModel", "Error: ", e)
872:         }
873:         triggerNotification("🔔 تم إرسال الإشعار الموثوق بنجاح!")
874:     }
875:     fun approveRequest(request: PendingProviderEntity) = adminViewModel.approveRequest(request)
876:     fun rejectRequest(request: PendingProviderEntity, reason: String) = adminViewModel.rejectRequest(request, reason)
877:     fun approveTechnician(providerId: String) = adminViewModel.approveTechnician(providerId)
878:     fun loadPendingTechnicians() = adminViewModel.loadPendingTechnicians()
879:     fun approvePendingProvider(pending: PendingProviderEntity) = adminViewModel.approvePendingProvider(pending)
880:     fun saveStore(store: com.example.data.StoreEntity) = adminViewModel.saveStore(store)
881:     fun deleteStore(storeId: String) = adminViewModel.deleteStore(storeId)
882:     fun restoreStore(storeId: String) = adminViewModel.restoreStore(storeId)
883:     fun deleteStorePermanently(storeId: String) = adminViewModel.deleteStorePermanently(storeId)
884:     fun setStoreActive(storeId: String, isActive: Boolean) = adminViewModel.setStoreActive(storeId, isActive)
885:     fun setStorePinned(storeId: String, isPinned: Boolean) = adminViewModel.setStorePinned(storeId, isPinned)
886:     fun setStoreVip(storeId: String, isVip: Boolean) = adminViewModel.setStoreVip(storeId, isVip)
887:     fun setStoreVerified(storeId: String, isVerified: Boolean) = adminViewModel.setStoreVerified(storeId, isVerified)
888:     fun setStoreRecommended(storeId: String, isRecommended: Boolean) = adminViewModel.setStoreRecommended(storeId, isRecommended)
889:     fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) = adminViewModel.setStoreChatDisabled(storeId, isDisabled)
890:     fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) = adminViewModel.setStoreNotificationsDisabled(storeId, isDisabled)
891:     fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) = adminViewModel.setStorePaymentEnabled(storeId, isEnabled)
892:     fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) = adminViewModel.toggleStoreBlocked(storeId, isBlocked)
893:     fun toggleStoreActive(storeId: String) = adminViewModel.toggleStoreActive(storeId)
894:     fun toggleStorePinned(storeId: String) = adminViewModel.toggleStorePinned(storeId)
895:     fun toggleStoreChatDisabled(storeId: String) = adminViewModel.toggleStoreChatDisabled(storeId)
896:     fun approveStorePdf(storeId: String, approve: Boolean) = adminViewModel.approveStorePdf(storeId, approve)
897:     fun saveProperty(property: com.example.data.PropertyEntity) = adminViewModel.saveProperty(property)
898:     fun deleteProperty(propertyId: String) = adminViewModel.deleteProperty(propertyId)
899:     fun restoreProperty(propertyId: String) = adminViewModel.restoreProperty(propertyId)
900:     fun deletePropertyPermanently(propertyId: String) = adminViewModel.deletePropertyPermanently(propertyId)
901:     fun setPropertyActive(propertyId: String, isActive: Boolean) = adminViewModel.setPropertyActive(propertyId, isActive)
902:     fun setPropertyPinned(propertyId: String, isPinned: Boolean) = adminViewModel.setPropertyPinned(propertyId, isPinned)
903:     fun setPropertyVip(propertyId: String, isVip: Boolean) = adminViewModel.setPropertyVip(propertyId, isVip)
904:     fun setPropertyVerified(propertyId: String, isVerified: Boolean) = adminViewModel.setPropertyVerified(propertyId, isVerified)
905:     fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) = adminViewModel.setPropertyRecommended(propertyId, isRecommended)
906:     fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) = adminViewModel.setPropertyChatDisabled(propertyId, isDisabled)
907:     fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) = adminViewModel.setPropertyNotificationsDisabled(propertyId, isDisabled)
908:     fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) = adminViewModel.setPropertyPaymentEnabled(propertyId, isEnabled)
909:     fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) = adminViewModel.togglePropertyBlocked(propertyId, isBlocked)
910:     fun approvePropertyPdf(propertyId: String, approve: Boolean) = adminViewModel.approvePropertyPdf(propertyId, approve)
911:     fun saveJob(job: com.example.data.JobEntity) = adminViewModel.saveJob(job)
912:     fun deleteJob(jobId: String) = adminViewModel.deleteJob(jobId)
913:     fun restoreJob(jobId: String) = adminViewModel.restoreJob(jobId)
914:     fun deleteJobPermanently(jobId: String) = adminViewModel.deleteJobPermanently(jobId)
915:     fun setJobApproved(jobId: String, isApproved: Boolean) = adminViewModel.setJobApproved(jobId, isApproved)
916:     fun setJobPinned(jobId: String, isPinned: Boolean) = adminViewModel.setJobPinned(jobId, isPinned)
917:     fun setJobVip(jobId: String, isVip: Boolean) = adminViewModel.setJobVip(jobId, isVip)
918:     fun setJobChatDisabled(jobId: String, isDisabled: Boolean) = adminViewModel.setJobChatDisabled(jobId, isDisabled)
919:     fun submitJobApplication(application: com.example.data.JobApplicationEntity) = adminViewModel.submitJobApplication(application)
920:     fun updateJobApplicationStatus(appId: String, status: String) = adminViewModel.updateJobApplicationStatus(appId, status)
921:     fun acceptJobApplication(appId: String) = adminViewModel.acceptJobApplication(appId)
922:     fun rejectJobApplication(appId: String, reason: String) = adminViewModel.rejectJobApplication(appId, reason)
923:     fun deleteJobApplication(appId: String) = adminViewModel.deleteJobApplication(appId)
924:     fun deleteReport(reportId: String) = adminViewModel.deleteReport(reportId)
925:     fun sendReport(providerId: String, providerName: String, reporterName: String, content: String) = adminViewModel.sendReport(providerId, providerName, reporterName, content)
926:     fun saveCoupon(coupon: CouponEntity) = adminViewModel.saveCoupon(coupon)
927:     fun deleteCoupon(couponId: String) = adminViewModel.deleteCoupon(couponId)
928:     fun saveInternalWallet(wallet: com.example.data.InternalWalletEntity) = adminViewModel.saveInternalWallet(wallet)
929:     fun performWalletTransaction(
930:         walletId: String,
931:         ownerName: String,
932:         ownerPhone: String,
933:         ownerType: String,
934:         type: String, // DEPOSIT, WITHDRAWAL, TRANSFER
935:         amount: Double,
936:         note: String
937:     ) = adminViewModel.performWalletTransaction(walletId, ownerName, ownerPhone, ownerType, type, amount, note)
938:     fun addPaymentWallet(wallet: PaymentWalletEntity) = adminViewModel.addPaymentWallet(wallet)
939:     fun updatePaymentWallet(wallet: PaymentWalletEntity) = adminViewModel.updatePaymentWallet(wallet)
940:     fun deletePaymentWallet(walletId: String) = adminViewModel.deletePaymentWallet(walletId)
941:     fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) = adminViewModel.togglePaymentWalletVisibility(walletId, currentVisible)
942:     fun confirmPayment(
943:         paymentId: String,
944:         transferId: String,
945:         transferPhoto: String,
946:         walletProvider: String,
947:         walletNumber: String,
948:         walletAccountName: String
949:     ) = adminViewModel.confirmPayment(paymentId, transferId, transferPhoto, walletProvider, walletNumber, walletAccountName)
950:     fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) = adminViewModel.verifyPayment(paymentId, isVerified, note, adminName)
951:     fun refundPayment(paymentId: String, reason: String) = adminViewModel.refundPayment(paymentId, reason)
952:     fun saveProduct(product: com.example.data.ProductEntity) = adminViewModel.saveProduct(product)
953:     fun deleteProduct(productId: String) = adminViewModel.deleteProduct(productId)
954:     fun updateProductPrice(productId: String, newPrice: Double) = adminViewModel.updateProductPrice(productId, newPrice)
955:     fun saveOffer(offer: com.example.data.models.Offer) = adminViewModel.saveOffer(offer)
956:     fun deleteOffer(offerId: String) = adminViewModel.deleteOffer(offerId)
957:     fun toggleOfferStatus(offerId: String, isActive: Boolean) = adminViewModel.toggleOfferStatus(offerId, isActive)
958:     fun listenToOffersForEntity(
959:         entityId: String,
960:         onResult: (List<com.example.data.models.Offer>) -> Unit
961:     ): com.google.firebase.firestore.ListenerRegistration = adminViewModel.listenToOffersForEntity(entityId, onResult)
962:     fun listenToProductsForStore(
963:         storeId: String,
964:         onResult: (List<com.example.data.ProductEntity>) -> Unit
965:     ): com.google.firebase.firestore.ListenerRegistration = adminViewModel.listenToProductsForStore(storeId, onResult)
966:     fun saveCustomProfileTab(tab: com.example.data.CustomProfileTabEntity) = adminViewModel.saveCustomProfileTab(tab)
967:     fun deleteCustomProfileTab(tabId: String) = adminViewModel.deleteCustomProfileTab(tabId)
968:     fun toggleCustomProfileTab(tabId: String) = adminViewModel.toggleCustomProfileTab(tabId)
969:     fun deleteCategory(categoryId: String) = adminViewModel.deleteCategory(categoryId)
970:     fun togglePinCategory(categoryId: String) = adminViewModel.togglePinCategory(categoryId)
971:     fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) = adminViewModel.mergeCategories(sourceCategoryId, targetCategoryId)
972:     fun saveCategoryEntity(cat: CategoryEntity) = adminViewModel.saveCategoryEntity(cat)
973:     fun addSubCategory(parentId: String, nameAr: String, icon: String) = adminViewModel.addSubCategory(parentId, nameAr, icon)
974:     fun convertCategoryType(catId: String, newParentId: String, isMain: Boolean) = adminViewModel.convertCategoryType(catId, newParentId, isMain)
975:     fun reorderCategories(newOrderedList: List<CategoryEntity>) = adminViewModel.reorderCategories(newOrderedList)
976:     fun updateCity(city: CityEntity) = adminViewModel.updateCity(city)
977:     fun removeCity(cityId: String) = adminViewModel.removeCity(cityId)
978:     fun removeProvider(providerId: String) = adminViewModel.removeProvider(providerId)
979:     fun removeProviderPermanently(providerId: String) = adminViewModel.removeProviderPermanently(providerId)
980:     fun restoreProvider(providerId: String) = adminViewModel.restoreProvider(providerId)
981:     fun pinProvider(providerId: String, isPinned: Boolean) = adminViewModel.pinProvider(providerId, isPinned)
982:     fun recommendProvider(providerId: String, isRecommended: Boolean) = adminViewModel.recommendProvider(providerId, isRecommended)
983:     fun verifyProviderBadge(providerId: String, isVerified: Boolean) = adminViewModel.verifyProviderBadge(providerId, isVerified)
984:     fun toggleProviderSubscription(providerId: String, status: String) = adminViewModel.toggleProviderSubscription(providerId, status)
985:     fun setProviderChatDisabled(providerId: String, disabled: Boolean) = adminViewModel.setProviderChatDisabled(providerId, disabled)
986:     fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) = adminViewModel.setProviderNotificationsDisabled(providerId, disabled)
987:     fun setProviderPaymentRequired(providerId: String, required: Boolean) = adminViewModel.setProviderPaymentRequired(providerId, required)
988:     fun extendProviderSubscription(providerId: String, extraMs: Long) = adminViewModel.extendProviderSubscription(providerId, extraMs)
989:     fun toggleProviderBlock(providerId: String) = adminViewModel.toggleProviderBlock(providerId)
990:     fun toggleProviderStatus(provider: ProviderEntity) = adminViewModel.toggleProviderStatus(provider)
991:     fun toggleProviderPin(providerId: String) = adminViewModel.toggleProviderPin(providerId)
992:     fun toggleProviderVerification(providerId: String) = adminViewModel.toggleProviderVerification(providerId)
993:     fun toggleProviderRecommendation(providerId: String) = adminViewModel.toggleProviderRecommendation(providerId)
994:     fun updateProviderEntity(provider: ProviderEntity) = adminViewModel.updateProviderEntity(provider)
995:     fun updateStoreEntity(store: StoreEntity) {
996:         db.collection("stores").document(store.id).set(store)
997:         triggerNotification("✅ تم تحديث بيانات المتجر بنجاح")
998:     }
999:     fun updatePropertyEntity(property: PropertyEntity) {
1000:         db.collection("properties").document(property.id).set(property)
1001:         triggerNotification("✅ تم تحديث بيانات العقار بنجاح")
1002:     }
1003:     fun updateBusinessAccountStatus(accountId: String, isActive: Boolean) {
1004:         viewModelScope.launch {
1005:             try {
1006:                 db.collection("stores").document(accountId).update("isActive", isActive)
1007:             } catch (e: Exception) {
1008:                 android.util.Log.e("MainViewModel", "Error: ", e)
1009:             }
1010:             try {
1011:                 db.collection("providers").document(accountId).update("isAvailable", isActive)
1012:             } catch (e: Exception) {
1013:                 android.util.Log.e("MainViewModel", "Error: ", e)
1014:             }
1015:         }
1016:     }
1017:     fun updateEntityImages(collection: String, id: String, profileImg: String, coverImg: String) {
1018:         val updates = mutableMapOf<String, Any>()
1019:         if (profileImg.isNotBlank()) {
1020:             updates["profileImage"] = profileImg
1021:             updates["logoImage"] = profileImg
1022:         }
1023:         if (coverImg.isNotBlank()) {
1024:             updates["coverImage"] = coverImg
1025:         }
1026:         if (updates.isNotEmpty()) {
1027:             db.collection(collection).document(id).update(updates)
1028:             triggerNotification("📸 تم تحديث الصور بنجاح")
1029:         }
1030:     }
1031:     fun editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) = adminViewModel.editProviderPhoneAndCategory(providerId, newPhone, newCategoryId)
1032:     fun addNewProvider(name: String, phone: String, catId: String, area: String, price: Double, isVip: Boolean) = adminViewModel.addNewProvider(name, phone, catId, area, price, isVip)
1033:     fun addNewProviderCustom(
1034:         name: String,
1035:         phone: String,
1036:         catId: String,
1037:         street: String,
1038:         cityId: String,
1039:         profileImage: String,
1040:         idCardImage: String,
1041:         forensicImage: String,
1042:         price: Double,
1043:         isVip: Boolean
1044:     ) = adminViewModel.addNewProviderCustom(name, phone, catId, street, cityId, profileImage, idCardImage, forensicImage, price, isVip)
1045:     fun deleteBanner(bannerId: String) = adminViewModel.deleteBanner(bannerId)
1046:     fun reorderBanners(newOrderedList: List<BannerEntity>) = adminViewModel.reorderBanners(newOrderedList)
1047:     fun placeOrder(order: com.example.data.OrderEntity) = adminViewModel.placeOrder(order)
1048:     fun updateOrderStatus(orderId: String, status: String) = adminViewModel.updateOrderStatus(orderId, status)
1049:     fun deleteOrder(orderId: String) = adminViewModel.deleteOrder(orderId)
1050:     fun deleteAllOrders(customerPhone: String) = adminViewModel.deleteAllOrders(customerPhone)
1051:     fun addRating(rating: com.example.data.RatingEntity) = adminViewModel.addRating(rating)
1052:     fun addRatingReply(ratingId: String, replyText: String) = adminViewModel.addRatingReply(ratingId, replyText)
1053:     fun deleteRating(ratingId: String) = adminViewModel.deleteRating(ratingId)
1054:     fun approveRating(ratingId: String, isApproved: Boolean) = adminViewModel.approveRating(ratingId, isApproved)
1055:     fun submitRating(providerId: String, rating: Int) = adminViewModel.submitRating(providerId, rating)
1056:     fun recalculateTargetRating(targetId: String, targetType: String) = adminViewModel.recalculateTargetRating(targetId, targetType)
1057:     fun logAdminActivity(action: String) = adminViewModel.logAdminActivity(action)
1058:     fun logCall(providerId: String, providerName: String) = adminViewModel.logCall(providerId, providerName)
1059:     fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? = adminViewModel.checkAndGetDuplicateAccountType(phone, excludeId)
1060:     fun updateProviderPortfolio(providerId: String, images: List<String>) = adminViewModel.updateProviderPortfolio(providerId, images)
1061:     fun addPortfolioImage(providerId: String, imageBase64: String) = adminViewModel.addPortfolioImage(providerId, imageBase64)
1062:     fun removePortfolioImage(providerId: String, index: Int) = adminViewModel.removePortfolioImage(providerId, index)
1063:     fun clearPortfolio(providerId: String) = adminViewModel.clearPortfolio(providerId)
1064:     fun redirectBookingToEntity(bookingId: String, targetEntityId: String, targetEntityName: String, targetPhone: String) = adminViewModel.redirectBookingToEntity(bookingId, targetEntityId, targetEntityName, targetPhone)
1065:     fun unbanEntity(entityType: String, entityId: String) = adminViewModel.unbanEntity(entityType, entityId)
1066:     fun restoreEntity(entityType: String, entityId: String) = adminViewModel.restoreEntity(entityType, entityId)
1067:     fun hardDeleteEntity(entityType: String, entityId: String) = adminViewModel.hardDeleteEntity(entityType, entityId)
1068:     fun exportJobApplicantsCsv(context: android.content.Context) = adminViewModel.exportJobApplicantsCsv(context)
1069:     fun loadCardSettings() = settingsViewModel.loadCardSettings()
1070:     fun updateCardSettings(settings: com.example.ui.viewmodels.SettingsViewModel.CardSettings) = settingsViewModel.updateCardSettings(settings)
1071:     fun updateTheme(themeId: String) = settingsViewModel.updateTheme(themeId)
1072:     fun saveCustomSettingsState(newSettings: AdminSettingsEntity) = settingsViewModel.saveCustomSettingsState(newSettings)
1073:     fun updateAdminSettings(newSettings: AdminSettingsEntity) = settingsViewModel.updateAdminSettings(newSettings)
1074:     fun initColorSync(context: android.content.Context) = settingsViewModel.initColorSync(context)
1075:     fun updateCloudColorScheme(context: android.content.Context, newScheme: com.example.data.ColorSchemeEntity) = settingsViewModel.updateCloudColorScheme(context, newScheme)
1076:     fun updatePersonalColors(context: android.content.Context, personal: com.example.data.PersonalColors) = settingsViewModel.updatePersonalColors(context, personal)
1077:     fun triggerManualSync(context: android.content.Context) = settingsViewModel.triggerManualSync(context)
1078:     fun resolveConflict(context: android.content.Context, useCloud: Boolean) = settingsViewModel.resolveConflict(context, useCloud)
1079:     fun getCurrentTimestampString(): String = settingsViewModel.getCurrentTimestampString()
1080:     fun addNewSyncLog(
1081:         context: android.content.Context,
1082:         type: String,
1083:         status: String,
1084:         changes: List<String>,
1085:         versionFrom: Int,
1086:         versionTo: Int
1087:     ) = settingsViewModel.addNewSyncLog(context, type, status, changes, versionFrom, versionTo)
1088:     fun toggleChatParticipant(participantType: ChatParticipantType) = settingsViewModel.toggleChatParticipant(participantType)
1089:     fun isChatBlockedFor(participantType: ChatParticipantType): Boolean = settingsViewModel.isChatBlockedFor(participantType)
1090:     fun canParticipateInChat(participantType: ChatParticipantType): Boolean = settingsViewModel.canParticipateInChat(participantType)
1091:     fun startVoiceCall(name: String, role: String) = settingsViewModel.startVoiceCall(name, role)
1092:     fun endVoiceCall() = settingsViewModel.endVoiceCall()
1093:     fun exportComplaintsToCSV() = settingsViewModel.exportComplaintsToCSV()
1094:     fun exportComplaintsToPDF() = settingsViewModel.exportComplaintsToPDF()
1095:     fun exportPerformanceReportToPDF() = settingsViewModel.exportPerformanceReportToPDF()
1096:     fun createSystemBackup(onComplete: (Boolean, String) -> Unit) = settingsViewModel.createSystemBackup(onComplete)
1097:     fun restoreSystemFromBackup(jsonStr: String, onComplete: (Boolean, String) -> Unit) = settingsViewModel.restoreSystemFromBackup(jsonStr, onComplete)
1098:     fun exportSelectedCollectionsAsJson(selectedCollections: List<String>, onResult: (String) -> Unit) = settingsViewModel.exportSelectedCollectionsAsJson(selectedCollections, onResult)
1099:     fun saveBackupToLocalStorage(context: android.content.Context, jsonStr: String, fileName: String): String = settingsViewModel.saveBackupToLocalStorage(context, jsonStr, fileName)
1100:     fun setSecondaryFirebaseConfig(projectId: String, apiKey: String, appId: String, storageBucket: String, isEnabled: Boolean) = settingsViewModel.setSecondaryFirebaseConfig(projectId, apiKey, appId, storageBucket, isEnabled)
1101:     fun saveCustomPermissionsMatrixToFirestore(permissions: List<String>) = settingsViewModel.saveCustomPermissionsMatrixToFirestore(permissions)
1102:     fun deleteColorPalette(id: String) = settingsViewModel.deleteColorPalette(id)
1103:     fun resetAccountPassword(entityType: String, phoneOrId: String, newPass: String) = settingsViewModel.resetAccountPassword(entityType, phoneOrId, newPass)
1104:     fun requestAdminPasswordReset(phone: String) = settingsViewModel.requestAdminPasswordReset(phone)
1105:     fun requestPasswordReset(phone: String, onResult: (Boolean, String) -> Unit) = settingsViewModel.requestPasswordReset(phone, onResult)
1106:     fun approvePasswordReset(phone: String, onResult: (Boolean, String) -> Unit) = settingsViewModel.approvePasswordReset(phone, onResult)
1107:     fun adminResetAccountPassword(phone: String, newPassword: String, notifyAction: String, customerName: String) = settingsViewModel.adminResetAccountPassword(phone, newPassword, notifyAction, customerName)
1108:     fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) = settingsViewModel.requestPasswordRecoveryForStore(name, phone, password)
1109:     fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) = settingsViewModel.requestPasswordRecoveryForProperty(title, phone, password)
1110:     fun requestPasswordRecoveryGeneral(accountName: String, phone: String, accountType: String, currentPassword: String) = settingsViewModel.requestPasswordRecoveryGeneral(accountName, phone, accountType, currentPassword)
1111:     fun wipeAllDatabaseData(password: String): Boolean = settingsViewModel.wipeAllDatabaseData(password)
1112:     fun wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean = settingsViewModel.wipeSelectedDatabaseData(password, selectedCollections)
1113:     fun wipeAllMockAndTemporaryData() = settingsViewModel.wipeAllMockAndTemporaryData()
1114:     fun acceptRequestOffer(
1115:         req: com.example.data.models.InstantRequestEntity,
1116:         offer: com.example.data.models.RequestOfferEntity
1117:     ) = instantRequestViewModel.acceptRequestOffer(req, offer)
1118:     fun completeInstantRequest(requestId: String) = instantRequestViewModel.completeInstantRequest(requestId)
1119:     fun setPasswordRecoveryWaitingPhone(phone: String) = authViewModel.setPasswordRecoveryWaitingPhone(phone)
1120:     fun resetRegistrationState() = authViewModel.resetRegistrationState()
1121:     fun searchAccountForRestore(cleanPhone: String, onResult: (RestoreAccountMatch?) -> Unit) {
1122:         accountRecoveryHelper.searchAccountForRestore(cleanPhone, onResult)
1123:     }
1124:     fun requestPasswordReset(
1125:         context: android.content.Context,
1126:         phone: String,
1127:         name: String,
1128:         accountType: String,
1129:         onResult: (Boolean) -> Unit
1130:     ) {
1131:         accountRecoveryHelper.requestPasswordReset(
1132:             context = context,
1133:             phone = phone,
1134:             name = name,
1135:             accountType = accountType,
1136:             onPasswordWaitingPhoneSet = { setPasswordRecoveryWaitingPhone(it) },
1137:             triggerNotification = { triggerNotification(it) },
1138:             onResult = onResult
1139:         )
1140:     }
1141:     fun adminResolvePasswordReset(
1142:         context: android.content.Context,
1143:         phone: String,
1144:         newPassword: String,
1145:         onResult: (Boolean) -> Unit
1146:     ) {
1147:         accountRecoveryHelper.adminResolvePasswordReset(
1148:             context = context,
1149:             phone = phone,
1150:             newPassword = newPassword,
1151:             onResult = onResult
1152:         )
1153:     }
1154:     fun isUserLoggedIn(context: android.content.Context): Boolean {
1155:         val isLoggedIn = preferenceHelper.isAccountLoggedIn(context)
1156:         val phone = currentUserPhone.value
1157:         return isLoggedIn || (phone.isNotBlank() && currentUserId.value != "guest" && currentUserId.value.isNotBlank())
1158:     }
1159:     fun restoreUserAccountByPhoneAndPassword(
1160:         context: android.content.Context,
1161:         phone: String,
1162:         password: String,
1163:         onResult: (Boolean, String) -> Unit
1164:     ) {
1165:         val email = getAuthEmailForPhone(phone)
1166:         auth.signInWithEmailAndPassword(email, password)
1167:             .addOnSuccessListener { authResult ->
1168:                 val userId = authResult.user?.uid ?: ""
1169:                 db.collection("users").document(userId).update("isDeleted", false)
1170:                 db.collection("providers").document(phone).update("isDeleted", false)
1171:                 db.collection("stores").document(phone).update("isDeleted", false)
1172:                 db.collection("properties").document(phone).update("isDeleted", false)
1173:                 onResult(true, "تم استعادة الحساب بنجاح!")
1174:             }
1175:             .addOnFailureListener { e ->
1176:                 onResult(false, e.message ?: "فشل في تسجيل الدخول")
1177:             }
1178:     }
1179:     fun restoreGuestUser(context: android.content.Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
1180:         restoreUserAccountByPhoneAndPassword(context, phone, password, onResult)
1181:     }
1182:     fun loginUserDirectly(context: android.content.Context, phone: String) = authViewModel.loginUserDirectly(context, phone)
1183:     fun showBackdoorDialog() = authViewModel.showBackdoorDialog()
1184:     fun dismissBackdoorDialog() = authViewModel.dismissBackdoorDialog()
1185:     fun setSupervisorSession(sup: SupervisorEntity) = authViewModel.setSupervisorSession(sup)
1186:     fun hasAdminPermission(permissionKey: String): Boolean = authViewModel.hasAdminPermission(permissionKey)
1187:     fun updateSupervisorPermissions(id: String, permissions: List<String>) = authViewModel.updateSupervisorPermissions(id, permissions)
1188:     fun removeSupervisor(id: String) = authViewModel.removeSupervisor(id)
1189:     fun getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
1190:         viewModelScope.launch {
1191:             chatRepo.getOrCreateChannel(
1192:                 currentUserId = providerId,
1193:                 currentUserName = providerName,
1194:                 currentUserPhoto = "",
1195:                 otherUserId = customerId,
1196:                 otherUserName = customerName,
1197:                 otherUserPhoto = "",
1198:                 type = com.example.data.models.ChannelType.PRIVATE,
1199:                 relatedEntityId = null,
1200:                 relatedEntityType = null
1201:             )
1202:         }
1203:     }
1204:     fun deleteChatChannel(channelId: String) {
1205:         viewModelScope.launch {
1206:             chatRepo.deleteChannel(channelId)
1207:             triggerToast("تم حذف المحادثة")
1208:         }
1209:     }
1210:     fun toggleBlockChatChannel(channelId: String) {
1211:         viewModelScope.launch {
1212:             val ch = _chatChannels.value.find { it.id == channelId } ?: return@launch
1213:             blockChatChannel(channelId, !ch.isBlocked)
1214:         }
1215:     }
1216:     fun blockChatChannel(channelId: String, blocked: Boolean) {
1217:         db.collection("chat_channels").document(channelId).update("isBlocked", blocked)
1218:         _activeChatChannel.value = _activeChatChannel.value?.copy(isBlocked = blocked)
1219:     }
1220:     fun wipeOldChatChannels(days: Int) {
1221:         triggerToast("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
1222:     }
1223:     fun updateBookingFormFields(fields: BookingFormFields) = bookingViewModel.updateBookingFormFields(fields)
1224:     fun updateDistributionMode(mode: BookingDistributionMode) = bookingViewModel.updateDistributionMode(mode)
1225:     fun cancelBookingByUser(bookingId: String) = bookingViewModel.cancelBookingByUser(bookingId)
1226:     fun getBookingStatusColor(status: String): String = bookingViewModel.getBookingStatusColor(status)
1227:     fun getBookingStatusLabel(status: String): String = bookingViewModel.getBookingStatusLabel(status)
1228:     fun getBookingProgress(status: String): Float = bookingViewModel.getBookingProgress(status)
1229:     fun markNotificationAsRead(context: android.content.Context, notifId: String) {
1230:         val currentRead = _readNotificationIds.value.toMutableSet()
1231:         val updated = preferenceHelper.markNotificationAsRead(context, notifId, currentRead)
1232:         _readNotificationIds.value = updated
1233:     }
1234:     fun loadReadNotifications(context: android.content.Context) {
1235:         _readNotificationIds.value = preferenceHelper.getReadNotificationIds(context)
1236:     }
1237:     fun markAllNotificationsAsRead(context: android.content.Context) {
1238:         val allIds = _notifications.value.map { it.id }.toSet()
1239:         preferenceHelper.markAllNotificationsAsRead(context, allIds)
1240:         _readNotificationIds.value = allIds
1241:     }
1242:     fun deleteNotification(notifId: String) {
1243:         db.collection("notifications").document(notifId).delete()
1244:         _notifications.value = _notifications.value.filter { it.id != notifId }
1245:         val currentRead = _readNotificationIds.value.toMutableSet()
1246:         currentRead.remove(notifId)
1247:         _readNotificationIds.value = currentRead
1248:     }
1249:     fun deleteAllNotifications() {
1250:         val allNotifs = _notifications.value
1251:         _notifications.value = emptyList()
1252:         _readNotificationIds.value = emptySet()
1253:         allNotifs.forEach { notif ->
1254:             db.collection("notifications").document(notif.id).delete()
1255:         }
1256:     }
1257:     fun clearAllNotifications() {
1258:         deleteAllNotifications()
1259:     }
1260:     val isProviderUser: Boolean get() = adminRole.value == "PROVIDER" || adminRole.value == "TECHNICIAN"
1261:     val _currentSupervisorPermissions get() = authViewModel._currentSupervisorPermissions
1262:     val currentSupervisorPermissions get() = authViewModel.currentSupervisorPermissions
1263:     fun openSupportChat() {
1264:         val currentUserId = authViewModel.getOrGenerateUserId()
1265:         val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
1266:         val currentUserPhoto = ""
1267:         
1268:         viewModelScope.launch {
1269:             val result = chatRepo.getOrCreateChannel(
1270:                 currentUserId = currentUserId,
1271:                 currentUserName = currentUserName,
1272:                 currentUserPhoto = currentUserPhoto,
1273:                 otherUserId = com.example.data.repositories.ChatRepository.SUPPORT_ADMIN_ID,
1274:                 otherUserName = com.example.data.repositories.ChatRepository.SUPPORT_ADMIN_NAME,
1275:                 otherUserPhoto = "",
1276:                 type = com.example.data.models.ChannelType.SUPPORT,
1277:                 relatedEntityId = null,
1278:                 relatedEntityType = null
1279:             )
1280:             if (result is AppResult.Success) {
1281:                 targetChatChannelId = result.data.id
1282:                 navigateToScreen("CHAT_DIRECT")
1283:             }
1284:         }
1285:     }
1286:     fun openChatChannel(channel: ChatChannelEntity?) {
1287:         if (channel == null) return
1288:         val currentUserId = authViewModel.getOrGenerateUserId()
1289:         val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
1290:         val currentUserPhoto = ""
1291:         
1292:         val otherUserId = if (channel.customerId == currentUserId) channel.targetId else channel.customerId
1293:         val otherUserName = if (channel.customerId == currentUserId) channel.targetName else channel.customerName
1294:         
1295:         viewModelScope.launch {
1296:             val result = chatRepo.getOrCreateChannel(
1297:                 currentUserId = currentUserId,
1298:                 currentUserName = currentUserName,
1299:                 currentUserPhoto = currentUserPhoto,
1300:                 otherUserId = otherUserId,
1301:                 otherUserName = otherUserName,
1302:                 otherUserPhoto = "",
1303:                 type = com.example.data.models.ChannelType.PRIVATE,
1304:                 relatedEntityId = null,
1305:                 relatedEntityType = null
1306:             )
1307:             if (result is AppResult.Success) {
1308:                 targetChatChannelId = result.data.id
1309:                 // Note: The caller usually navigates, so we don't navigate here.
1310:             }
1311:         }
1312:     }
1313:     fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
1314:         return authViewModel.verifyAdminOrOwnerPassword(password, adminPass, ownerPass)
1315:     }
1316:     fun setUserSessionDetails(context: android.content.Context, name: String, phone: String, residence: String = "اليمن") {
1317:         authViewModel.setUserSessionDetails(context, name, phone, residence)
1318:     }
1319:     fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, password: String = "") {
1320:         authViewModel.registerGuestUser(context, name, phone, residence, password)
1321:     }
1322:     fun toggleFavorite(id: String) {
1323:         val current = _favoriteIds.value.toMutableSet()
1324:         if (current.contains(id)) {
1325:             current.remove(id)
1326:             triggerNotification("💔 تم إزالة العنصر من المفضلة")
1327:         } else {
1328:             current.add(id)
1329:             triggerNotification("💖 تم إضافة العنصر إلى المفضلة")
1330:         }
1331:         _favoriteIds.value = current
1332:     }
1333:     fun toggleBlockStore(storeId: String) {
1334:         val store = _stores.value.find { it.id == storeId }
1335:         if (store != null) {
1336:             adminViewModel.toggleStoreBlocked(storeId, !store.isBlocked)
1337:         }
1338:     }
1339:     fun addBooking(
1340:         name: String,
1341:         phone: String,
1342:         area: String,
1343:         serviceType: String,
1344:         providerId: String,
1345:         providerName: String,
1346:         dateString: String,
1347:         timeString: String,
1348:         couponCode: String = "",
1349:         pinCode: String = "",
1350:         customBookingId: String = "",
1351:         customPassword: String = ""
1352:     ) = bookingViewModel.addBooking(
1353:         name = name,
1354:         phone = phone,
1355:         area = area,
1356:         serviceType = serviceType,
1357:         providerId = providerId,
1358:         providerName = providerName,
1359:         dateString = dateString,
1360:         timeString = timeString,
1361:         couponCode = couponCode,
1362:         pinCode = pinCode,
1363:         customBookingId = customBookingId,
1364:         customPassword = customPassword
1365:     )
1366:     fun addNewStore(
1367:         name: String,
1368:         phone: String,
1369:         cityId: String,
1370:         localNeighborhood: String,
1371:         categoryId: String,
1372:         coverImage: String,
1373:         workingHours: String
1374:     ) {
1375:         val newStore = StoreEntity(
1376:             id = UUID.randomUUID().toString(),
1377:             name = name,
1378:             phone = phone,
1379:             cityId = cityId,
1380:             localNeighborhood = localNeighborhood,
1381:             categoryId = categoryId,
1382:             coverImage = coverImage,
1383:             workingHours = workingHours,
1384:             isApproved = true,
1385:             createdAt = System.currentTimeMillis()
1386:         )
1387:         adminViewModel.saveStore(newStore)
1388:     }
1389:     fun openOrCreateChatChannel(
1390:         targetId: String,
1391:         targetType: String,
1392:         targetName: String,
1393:         targetPhone: String = "",
1394:         targetCategory: String = "",
1395:         relatedEntityId: String = "",
1396:         relatedEntityType: String = "",
1397:         onCreated: (ChatChannelEntity?) -> Unit
1398:     ) {
1399:         val currentUserId = authViewModel.getOrGenerateUserId()
1400:         val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
1401:         val currentUserPhoto = ""
1402:         
1403:         viewModelScope.launch {
1404:             val result = chatRepo.getOrCreateChannel(
1405:                 currentUserId = currentUserId,
1406:                 currentUserName = currentUserName,
1407:                 currentUserPhoto = currentUserPhoto,
1408:                 otherUserId = targetId,
1409:                 otherUserName = targetName,
1410:                 otherUserPhoto = "",
1411:                 type = com.example.data.models.ChannelType.PRIVATE,
1412:                 relatedEntityId = relatedEntityId.takeIf { it.isNotBlank() },
1413:                 relatedEntityType = relatedEntityType.takeIf { it.isNotBlank() }
1414:             )
1415:             
1416:             if (result is AppResult.Success) {
1417:                 targetChatChannelId = result.data.id
1418:                 val dummy = ChatChannelEntity(id = result.data.id)
1419:                 onCreated(dummy)
1420:             } else {
1421:                 onCreated(null)
1422:             }
1423:         }
1424:     }
1425:     fun sendNotificationToApplicants(title: String, message: String, jobId: String = "") {
1426:         adminViewModel.sendNotificationToApplicants(title, message, jobId)
1427:     }
1428:     fun createInstantRequest(
1429:         userId: String,
1430:         userName: String,
1431:         userPhone: String,
1432:         userCity: String,
1433:         userNeighborhood: String,
1434:         categoryId: String,
1435:         categoryName: String,
1436:         serviceTitle: String,
1437:         description: String,
1438:         images: List<String> = emptyList(),
1439:         urgencyTime: String = "فوراً (خلال 30 دقيقة)",
1440:         deliveryMethod: String = "",
1441:         customPin: String = "",
1442:         onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
1443:     ) {
1444:         instantRequestViewModel.createInstantRequest(
1445:             userId, userName, userPhone, userCity, userNeighborhood, categoryId, categoryName, serviceTitle, description, images, urgencyTime, deliveryMethod, customPin, onResult
1446:         )
1447:     }
1448:     fun submitOfferForRequest(
1449:         requestId: String,
1450:         requestCode: String,
1451:         technicianId: String,
1452:         technicianName: String,
1453:         technicianPhone: String,
1454:         technicianAvatar: String,
1455:         technicianRating: Float,
1456:         price: Double,
1457:         estimatedArrivalTime: String = "خلال 30 دقيقة",
1458:         estimatedDuration: String = "ساعتان",
1459:         notes: String = ""
1460:     ) {
1461:         instantRequestViewModel.submitOfferForRequest(
1462:             requestId, requestCode, technicianId, technicianName, technicianPhone, technicianAvatar, technicianRating, price, estimatedArrivalTime, estimatedDuration, notes
1463:         )
1464:     }
1465:     fun cancelInstantRequest(requestId: String, userPin: String = "") {
1466:         instantRequestViewModel.cancelInstantRequest(requestId = requestId, userPin = userPin)
1467:     }
1468:     fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) =
1469:         adminViewModel.addNewCategory(nameAr, nameEn, icon, description, parentId, isMainCategory)
1470:     fun editCategory(categoryId: String, newName: String, newIcon: String, parentId: String = "", isMainCategory: Boolean = true) =
1471:         adminViewModel.editCategory(categoryId, newName, newIcon, parentId, isMainCategory)
1472:     fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") =
1473:         adminViewModel.setStoreBlocked(storeId, isBlocked, reason)
1474:     fun setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") =
1475:         adminViewModel.setPropertyBlocked(propertyId, isBlocked, reason)
1476:     fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") =
1477:         adminViewModel.setJobBlocked(jobId, isBlocked, reason)
1478:     fun approveRegisteredUser(userId: String, userName: String = "") =
1479:         adminViewModel.approveRegisteredUser(userId, userName)
1480:     fun toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") =
1481:         adminViewModel.toggleBlockRegisteredUser(userId, currentBlocked, userName)
1482:     fun deleteRegisteredUser(userId: String, userName: String = "") =
1483:         adminViewModel.deleteRegisteredUser(userId, userName)
1484:     fun addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
1485:         adminViewModel.addNewBanner(title, url, redirect, type, size, duration, displayTime)
1486:     fun addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
1487:         adminViewModel.addBanner(title, url, redirect, type, size, duration, displayTime)
1488:     fun createPayment(
1489:         userId: String,
1490:         providerId: String,
1491:         amount: Double,
1492:         method: String,
1493:         bookingId: String = "",
1494:         isLinkedToBooking: Boolean = false,
1495:         bookingServiceType: String = ""
1496:     ) = adminViewModel.createPayment(userId, providerId, amount, method, bookingId, isLinkedToBooking, bookingServiceType)
1497:     fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") =
1498:         bookingViewModel.updateBookingStatus(bookingId, newStatus, rejectionReason)
1499:     fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) =
1500:         bookingViewModel.updateBookingStatus(bookingId, newStatus)
1501:     fun replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String, imageUrl: String = "") {
1502:         if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
1503:         viewModelScope.launch {
1504:             chatRepo.sendMessage(channelId, senderId, senderName, msgText, if (imageUrl.isNotBlank()) com.example.data.models.MediaType.IMAGE else com.example.data.models.MediaType.TEXT, imageUrl, null, null, null)
1505:             triggerToast("تم إرسال الرد بنجاح")
1506:         }
1507:     }
1508:     fun updateBackdoorSettings(
1509:         appName: String, welcomeMsg: String, footerMsg: String, themeId: String,
1510:         supportPhone: String, supportEmail: String, supportWhatsapp: String,
1511:         isMaintenance: Boolean, hiddenFooter: Boolean, botHidden: Boolean, botSize: Int,
1512:         chatHidden: Boolean, chatSize: Int, radiusKm: Int, isSpeech: Boolean,
1513:         isDataSaver: Boolean, imgQuality: Int,
1514:         bookingTerms: String = "يرجى الالتزام التام بالمواعيد المحددة والتسعيرة المتفق عليها مع الفني.",
1515:         bookingLabelName: String = "الاسم الكامل للعميل",
1516:         bookingLabelPhone: String = "رقم هاتف العميل للتواصل (مثال: 777000111)",
1517:         bookingLabelArea: String = "المنطقة والحي السكني",
1518:         bookingLabelService: String = "تفاصيل ونوع الخدمة المطلوبة",
1519:         adminUsername: String = "mah73646@gmail.com",
1520:         adminPassword: String = "Maher@@--@@736462##",
1521:         customPrimaryHex: String = "#059669",
1522:         customSecondaryHex: String = "#115E59",
1523:         customBackgroundHex: String = "#0A0F0D",
1524:         customSurfaceHex: String = "#121D18"
1525:     ) = settingsViewModel.updateBackdoorSettings(
1526:         appName, welcomeMsg, footerMsg, themeId, supportPhone, supportEmail, supportWhatsapp,
1527:         isMaintenance, hiddenFooter, botHidden, botSize, chatHidden, chatSize, radiusKm, isSpeech,
1528:         isDataSaver, imgQuality, bookingTerms, bookingLabelName, bookingLabelPhone, bookingLabelArea,
1529:         bookingLabelService, adminUsername, adminPassword, customPrimaryHex, customSecondaryHex,
1530:         customBackgroundHex, customSurfaceHex
1531:     )
1532:     fun addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
1533:         authViewModel.addSupervisor(name, role, passcode, permissions)
1534:     fun editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
1535:         authViewModel.editSupervisor(id, name, role, passcode, permissions)
1536:     fun addColorPalette(name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") =
1537:         settingsViewModel.addColorPalette(name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
1538:     fun addNewCity(nameAr: String, nameEn: String, icon: String = "📍", photoUrl: String = "", sortOrder: Int = 0) =
1539:         adminViewModel.addNewCity(nameAr, nameEn, icon, photoUrl, sortOrder)
1540:     fun addCoupon(code: String, pointsValue: Int, expiryMs: Long, discountPercentage: Int = 0, maxUsageCount: Int = 100) =
1541:         adminViewModel.addCoupon(code, pointsValue, expiryMs, discountPercentage, maxUsageCount)
1542:     fun rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") =
1543:         adminViewModel.rejectTechnician(providerId, reason)
1544:     fun sendMessageInChat(msgText: String, imageUrl: String = "") {
1545:         if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
1546:         val currentUserId = authViewModel.getOrGenerateUserId()
1547:         val currentName = authViewModel.currentUserName.value.ifEmpty { "العميل" }
1548:         viewModelScope.launch {
1549:             val result = chatRepo.getOrCreateChannel(
1550:                 currentUserId = currentUserId,
1551:                 currentUserName = currentName,
1552:                 currentUserPhoto = "",
1553:                 otherUserId = "ADMIN",
1554:                 otherUserName = "الدعم الفني",
1555:                 otherUserPhoto = "",
1556:                 type = com.example.data.models.ChannelType.SUPPORT
1557:             )
1558:             if (result is AppResult.Success) {
1559:                 chatRepo.sendMessage(result.data.id, currentUserId, currentName, msgText, if (imageUrl.isNotBlank()) com.example.data.models.MediaType.IMAGE else com.example.data.models.MediaType.TEXT, imageUrl, null, null, null)
1560:                 addNotification(
1561:                     "💬 رسالة جديدة في الدعم الفني المباشر",
1562:                     "من العميل $currentName: ${msgText.ifEmpty { "📷 [صورة]" }}",
1563:                     "SUPERVISOR",
1564:                     currentUserId
1565:                 )
1566:             }
1567:         }
1568:     }
1569:     fun submitReport(report: com.example.data.ReportEntity, onComplete: () -> Unit = {}) =
1570:         adminViewModel.submitReport(report, onComplete)
1571:     fun deleteBooking(bookingId: String) =
1572:         bookingViewModel.deleteBooking(bookingId)
1573:     fun updateBooking(booking: com.example.data.BookingEntity) =
1574:         bookingViewModel.updateBooking(booking)
1575:     fun submitRating(ratingEntity: com.example.data.RatingEntity, onComplete: () -> Unit = {}) =
1576:         adminViewModel.submitRating(ratingEntity, onComplete)
"""

text = re.sub(r'^\d+:\s', '', content, flags=re.MULTILINE)

text = re.sub(r'^(\s*)fun\s+([a-zA-Z0-9_]+)\s*\(', r'\1fun MainViewModel.\2(', text, flags=re.MULTILINE)
text = re.sub(r'^(\s*)val\s+([a-zA-Z0-9_]+)', r'\1val MainViewModel.\2', text, flags=re.MULTILINE)
text = re.sub(r'^(\s*)var\s+([a-zA-Z0-9_]+)', r'\1var MainViewModel.\2', text, flags=re.MULTILINE)
text = re.sub(r'^(\s*)private\s+var\s+([a-zA-Z0-9_]+)', r'\1var MainViewModel.\2', text, flags=re.MULTILINE)

imports = """package com.example.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.data.models.*
import com.example.data.*
import java.util.UUID
import com.example.utils.AppResult
import com.example.ui.viewmodels.SettingsViewModel.ChatParticipantType
import com.example.ui.viewmodels.BookingFormFields
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingStatus
import com.example.utils.LocaleManager

"""

with open('app/src/main/java/com/example/ui/MainViewModelDelegates.kt', 'w') as f:
    f.write(imports + text)

