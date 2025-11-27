package bubei.tingshu.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RemoveFromQueue
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import bubei.tingshu.APApplication
import bubei.tingshu.BuildConfig
import bubei.tingshu.Natives
import bubei.tingshu.R
import bubei.tingshu.ui.component.SwitchItem
import bubei.tingshu.ui.component.rememberConfirmDialog
import bubei.tingshu.ui.component.rememberLoadingDialog
import bubei.tingshu.ui.theme.refreshTheme
import bubei.tingshu.util.APatchKeyHelper
import bubei.tingshu.util.getBugreportFile
import bubei.tingshu.util.isForceUsingOverlayFS
import bubei.tingshu.util.isGlobalNamespaceEnabled
import bubei.tingshu.util.isLiteModeEnabled
import bubei.tingshu.util.outputStream
import bubei.tingshu.util.overlayFsAvailable
import bubei.tingshu.util.rootShellForResult
import bubei.tingshu.util.setForceUsingOverlayFS
import bubei.tingshu.util.setGlobalNamespaceEnabled
import bubei.tingshu.util.setLiteMode
import bubei.tingshu.util.ui.APDialogBlurBehindUtils
import bubei.tingshu.util.ui.LocalSnackbarHost
import bubei.tingshu.util.ui.NavigationBarsSpacer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Destination<RootGraph>
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen() {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
    var isGlobalNamespaceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var isLiteModeEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var forceUsingOverlayFS by rememberSaveable {
        mutableStateOf(false)
    }
    var bSkipStoreSuperKey by rememberSaveable {
        mutableStateOf(APatchKeyHelper.shouldSkipStoreSuperKey())
    }
    val isOverlayFSAvailable by rememberSaveable {
        mutableStateOf(overlayFsAvailable())
    }
    if (kPatchReady && aPatchReady) {
        isGlobalNamespaceEnabled = isGlobalNamespaceEnabled()
        isLiteModeEnabled = isLiteModeEnabled()
        forceUsingOverlayFS = isForceUsingOverlayFS()
    }

    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) }, // 对应“设置”字符串，应用名称已在strings.xml中设为“懒人听书”
            )
        },
        snackBarHost = { SnackbarHost(snackBarHost) }
    ) { paddingValues ->

        val loadingDialog = rememberLoadingDialog()
        val clearKeyDialog = rememberConfirmDialog(
            onConfirm = {
                APatchKeyHelper.clearConfigKey()
                APApplication.superKey = ""
            }
        )

        val showLanguageDialog = rememberSaveable { mutableStateOf(false) }
        LanguageDialog(showLanguageDialog)

        val showResetSuPathDialog = remember { mutableStateOf(false) }
        if (showResetSuPathDialog.value) {
            ResetSUPathDialog(showResetSuPathDialog)
        }

        val showThemeChooseDialog = remember { mutableStateOf(false) }
        if (showThemeChooseDialog.value) {
            ThemeChooseDialog(showThemeChooseDialog)
        }

        var showLogBottomSheet by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val logSavedMessage = stringResource(R.string.log_saved)
        val exportBugreportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/gzip")
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    loadingDialog.show()
                    uri.outputStream().use { output ->
                        getBugreportFile(context).inputStream().use {
                            it.copyTo(output)
                        }
                    }
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(message = logSavedMessage)
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = APApplication.sharedPreferences

            // 清除SuperKey
            if (kPatchReady) {
                val clearKeyDialogTitle = stringResource(id = R.string.clear_super_key)
                val clearKeyDialogContent =
                    stringResource(id = R.string.settings_clear_super_key_dialog)
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Key, stringResource(id = R.string.super_key)
                        )
                    },
                    headlineContent = { Text(stringResource(id = R.string.clear_super_key)) },
                    modifier = Modifier.clickable {
                        clearKeyDialog.showConfirm(
                            title = clearKeyDialogTitle,
                            content = clearKeyDialogContent,
                            markdown = false,
                        )
                    })
            }

            // 本地不存储SuperKey
            SwitchItem(
                icon = Icons.Filled.Key,
                title = stringResource(id = R.string.settings_donot_store_superkey),
                summary = stringResource(id = R.string.settings_donot_store_superkey_summary),
                checked = bSkipStoreSuperKey,
                onCheckedChange = {
                    bSkipStoreSuperKey = it
                    APatchKeyHelper.setShouldSkipStoreSuperKey(bSkipStoreSuperKey)
                })

            // 全局挂载模式
            if (kPatchReady && aPatchReady) {
                SwitchItem(
                    icon = Icons.Filled.Engineering,
                    title = stringResource(id = R.string.settings_global_namespace_mode),
                    summary = stringResource(id = R.string.settings_global_namespace_mode_summary),
                    checked = isGlobalNamespaceEnabled,
                    onCheckedChange = {
                        setGlobalNamespaceEnabled(
                            if (isGlobalNamespaceEnabled) {
                                "0"
                            } else {
                                "1"
                            }
                        )
                        isGlobalNamespaceEnabled = it
                    })
            }

            // 精简模式
            if (kPatchReady && aPatchReady) {
                SwitchItem(
                    icon = Icons.Filled.RemoveFromQueue,
                    title = stringResource(id = R.string.settings_lite_mode),
                    summary = stringResource(id = R.string.settings_lite_mode_mode_summary),
                    checked = isLiteModeEnabled,
                    onCheckedChange = {
                        setLiteMode(it)
                        isLiteModeEnabled = it
                    })
            }

            // 强制使用OverlayFS
            if (kPatchReady && aPatchReady && isOverlayFSAvailable) {
                SwitchItem(
                    icon = Icons.Filled.FilePresent,
                    title = stringResource(id = R.string.settings_force_overlayfs_mode),
                    summary = stringResource(id = R.string.settings_force_overlayfs_mode_summary),
                    checked = forceUsingOverlayFS,
                    onCheckedChange = {
                        setForceUsingOverlayFS(it)
                        forceUsingOverlayFS = it
                    })
            }

            // WebView调试
            if (aPatchReady) {
                var enableWebDebugging by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_web_debugging", false)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.DeveloperMode,
                    title = stringResource(id = R.string.enable_web_debugging),
                    summary = stringResource(id = R.string.enable_web_debugging_summary),
                    checked = enableWebDebugging
                ) {
                    APApplication.sharedPreferences.edit {
                        putBoolean("enable_web_debugging", it)
                    }
                    enableWebDebugging = it
                }
            }

            // 检查更新
            var checkUpdate by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("check_update", true)
                )
            }
            SwitchItem(
                icon = Icons.Filled.Update,
                title = stringResource(id = R.string.settings_check_update),
                summary = stringResource(id = R.string.settings_check_update_summary),
                checked = checkUpdate
            ) {
                prefs.edit { putBoolean("check_update", it) }
                checkUpdate = it
            }

            // 深色主题跟随系统
            var nightFollowSystem by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("night_mode_follow_sys", true)
                )
            }
            SwitchItem(
                icon = Icons.Filled.InvertColors,
                title = stringResource(id = R.string.settings_night_mode_follow_sys),
                summary = stringResource(id = R.string.settings_night_mode_follow_sys_summary),
                checked = nightFollowSystem
            ) {
                prefs.edit { putBoolean("night_mode_follow_sys", it) }
                nightFollowSystem = it
                refreshTheme.value = true
            }

            // 自定义深色主题开关
            if (!nightFollowSystem) {
                var nightThemeEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("night_mode_enabled", false)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(id = R.string.settings_night_theme_enabled),
                    checked = nightThemeEnabled
                ) {
                    prefs.edit { putBoolean("night_mode_enabled", it) }
                    nightThemeEnabled = it
                    refreshTheme.value = true
                }
            }

            // 系统动态颜色主题
            val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            if (isDynamicColorSupport) {
                var useSystemDynamicColor by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("use_system_color_theme", true)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.ColorLens,
                    title = stringResource(id = R.string.settings_use_system_color_theme),
                    summary = stringResource(id = R.string.settings_use_system_color_theme_summary),
                    checked = useSystemDynamicColor
                ) {
                    prefs.edit { putBoolean("use_system_color_theme", it) }
                    useSystemDynamicColor = it
                    refreshTheme.value = true
                }

                if (!useSystemDynamicColor) {
                    ListItem(headlineContent = {
                        Text(text = stringResource(id = R.string.settings_custom_color_theme))
                    }, modifier = Modifier.clickable {
                        showThemeChooseDialog.value = true
                    }, supportingContent = {
                        val colorMode = prefs.getString("custom_color", "blue")
                        Text(
                            text = stringResource(colorNameToString(colorMode.toString())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }, leadingContent = { Icon(Icons.Filled.FormatColorFill, null) })
                }
            } else {
                ListItem(headlineContent = {
                    Text(text = stringResource(id = R.string.settings_custom_color_theme))
                }, modifier = Modifier.clickable {
                    showThemeChooseDialog.value = true
                }, supportingContent = {
                    val colorMode = prefs.getString("custom_color", "blue")
                    Text(
                        text = stringResource(colorNameToString(colorMode.toString())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }, leadingContent = { Icon(Icons.Filled.FormatColorFill, null) })
            }

            // 重置su路径
            if (kPatchReady) {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Commit, stringResource(id = R.string.setting_reset_su_path)
                        )
                    },
                    supportingContent = {},
                    headlineContent = { Text(stringResource(id = R.string.setting_reset_su_path)) },
                    modifier = Modifier.clickable {
                        showResetSuPathDialog.value = true
                    })
            }

            // 语言设置
            ListItem(headlineContent = {
                Text(text = stringResource(id = R.string.settings_app_language))
            }, modifier = Modifier.clickable {
                showLanguageDialog.value = true
            }, supportingContent = {
                Text(text = AppCompatDelegate.getApplicationLocales()[0]?.displayLanguage?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                } ?: stringResource(id = R.string.system_default),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline)
            }, leadingContent = { Icon(Icons.Filled.Translate, null) })

            // 日志相关（保存/分享）
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.BugReport, stringResource(id = R.string.send_log)
                    )
                },
                headlineContent = { Text(stringResource(id = R.string.send_log)) },
                modifier = Modifier.clickable {
                    showLogBottomSheet = true
                })
            if (showLogBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLogBottomSheet = false },
                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                    content = {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {
                                                val formatter =
                                                    DateTimeFormatter.of
