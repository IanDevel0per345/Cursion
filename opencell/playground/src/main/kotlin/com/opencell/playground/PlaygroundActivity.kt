package com.opencell.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

class PlaygroundActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var tab by remember { mutableIntStateOf(0) }
                val titles = listOf("Mensagens", "Lista", "Formulário", "Diálogos")
                Column(Modifier.fillMaxSize().statusBarsPadding().semantics { testTagsAsResourceId = true }) {
                    TabRow(selectedTabIndex = tab) {
                        titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
                    }
                    when (tab) {
                        0 -> Messages()
                        1 -> LongList()
                        2 -> FormTab()
                        else -> Dialogs()
                    }
                }
            }
        }
    }
}

@Composable
private fun Messages() {
    var open by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var sent by remember { mutableIntStateOf(0) }
    var lastSent by remember { mutableStateOf("") }
    var sendLabel by remember { mutableStateOf("Enviar") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (open == null) {
            Text("Conversas", style = MaterialTheme.typography.titleLarge)
            listOf("Cliente A", "Cliente B", "Cliente C").forEach { name ->
                Text(name, Modifier.fillMaxWidth().clickable { open = name }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("Chat com $open", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth().semantics { testTag = "message_input" }, placeholder = { Text("Mensagem") })
            Button(onClick = { sent++; lastSent = draft; draft = "" }, Modifier.semantics { testTag = "send_button" }) { Text(sendLabel) }
            OutlinedButton(onClick = { sendLabel = if (sendLabel == "Enviar") "Submit" else "Enviar" }) { Text("Alterar texto do botão") }
            Text("Mensagens enviadas: $sent")
            if (sent > 0) Text("Última enviada: $lastSent")
            TextButton(onClick = { open = null }) { Text("Voltar às conversas") }
        }
    }
}

@Composable
private fun LongList() {
    LazyColumn(Modifier.fillMaxSize().semantics { testTag = "long_list" }, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        items((1..100).toList()) { n -> Text("Item $n", Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.bodyLarge) }
        item { Text("Fim da lista", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun FormTab() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var news by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("Receber novidades"); Switch(news, { news = it }) }
        Button(onClick = { saved = "Formulário salvo: $name" }) { Text("Salvar") }
        saved?.let { Text(it) }
    }
}

@Composable
private fun Dialogs() {
    var show by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Nenhuma ação") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { show = true }) { Text("Abrir diálogo") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { result = "Abrir (primeiro)" }) { Text("Abrir") }
            OutlinedButton(onClick = { result = "Abrir (segundo)" }) { Text("Abrir") }
        }
        Text("Resultado: $result")
    }
    if (show) AlertDialog(
        onDismissRequest = { show = false }, title = { Text("Confirmar ação?") }, text = { Text("Este é um diálogo de teste.") },
        confirmButton = { TextButton(onClick = { show = false; result = "Diálogo confirmado" }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { show = false; result = "Diálogo cancelado" }) { Text("Cancelar") } },
    )
}
