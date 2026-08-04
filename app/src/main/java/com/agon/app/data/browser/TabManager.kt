package com.agon.app.data.browser

import com.agon.app.ui.screens.browser.state.TabInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير التبويبات - يتعامل مع إضافة، حذف، وتنشيط التبويبات
 */
@Singleton
class TabManager @Inject constructor() {
    
    private val _tabs = MutableStateFlow<List<TabInfo>>(listOf(TabInfo.createNew()))
    val tabs: StateFlow<List<TabInfo>> = _tabs.asStateFlow()
    
    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()
    
    /**
     * إنشاء تبويب جديد وتنشيطه
     */
    fun createNewTab() {
        val newTab = TabInfo.createNew()
        _tabs.value = _tabs.value + newTab
        _activeTabIndex.value = _tabs.value.lastIndex
    }
    
    /**
     * إغلاق تبويب محدد
     */
    fun closeTab(tabId: String) {
        val currentIndex = _activeTabIndex.value
        val tabsList = _tabs.value.toMutableList()
        val removedIndex = tabsList.indexOfFirst { it.id == tabId }
        
        if (removedIndex >= 0) {
            tabsList.removeAt(removedIndex)
            
            // إذا كانت هذه آخر تبويب، أنشئ تبويباً جديداً
            if (tabsList.isEmpty()) {
                tabsList.add(TabInfo.createNew())
            }
            
            _tabs.value = tabsList
            
            // تحديث الفهرس النشط
            _activeTabIndex.value = when {
                tabsList.isEmpty() -> 0
                currentIndex >= tabsList.size -> tabsList.lastIndex
                currentIndex > removedIndex -> currentIndex - 1
                else -> currentIndex
            }
        }
    }
    
    /**
     * تنشيط تبويب محدد
     */
    fun activateTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeTabIndex.value = index
        }
    }
    
    /**
     * تحديث بيانات تبويب معين
     */
    fun updateTab(tabId: String, update: (TabInfo) -> TabInfo) {
        val tabsList = _tabs.value.toMutableList()
        val index = tabsList.indexOfFirst { it.id == tabId }
        
        if (index >= 0) {
            tabsList[index] = update(tabsList[index])
            _tabs.value = tabsList
        }
    }
    
    /**
     * التبويب النشط حالياً
     */
    fun getActiveTab(): TabInfo? {
        return _tabs.value.getOrNull(_activeTabIndex.value)
    }
    
    /**
     * إغلاق جميع التبويبات ما عدا النشط
     */
    fun closeOtherTabs() {
        val activeTab = getActiveTab() ?: return
        _tabs.value = listOf(activeTab)
        _activeTabIndex.value = 0
    }
}
