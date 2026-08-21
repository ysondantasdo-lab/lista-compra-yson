package br.com.yson.listacompras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShoppingListScreen(viewModel)
            }
        }
    }
}

// Cores com alto contraste, pensadas pra leitura fácil.
private val CorFundo = Color.White
private val CorTexto = Color.Black
private val CorBotaoAdicionar = Color(0xFF0057B8)
private val CorLixeira = Color(0xFFD32F2F)
private val CorSugestaoFundo = Color(0xFFEFEFEF)

@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val itens by viewModel.itens.collectAsState()
    val itensConhecidos by viewModel.itensConhecidos.collectAsState()

    var textoAtual by remember { mutableStateOf("") }

    val sugestoes = remember(textoAtual, itensConhecidos) {
        if (textoAtual.isBlank()) {
            emptyList()
        } else {
            itensConhecidos.filter {
                it.startsWith(textoAtual, ignoreCase = true) &&
                    !it.equals(textoAtual, ignoreCase = true)
            }.take(5)
        }
    }

    fun confirmarItem() {
        viewModel.adicionarItem(textoAtual)
        textoAtual = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CorFundo)
            .padding(16.dp)
    ) {
        // Lista numerada dos itens já lançados.
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(itens, key = { _, item -> item.id }) { index, item ->
                LinhaItem(
                    numero = index + 1,
                    texto = item.nome,
                    onApagar = { viewModel.removerItem(item.id) }
                )
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
        }

        // Linha fixa de entrada: número seguinte + campo de texto + botão "+".
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(
                text = "${itens.size + 1}.",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CorTexto,
                modifier = Modifier.width(44.dp)
            )

            OutlinedTextField(
                value = textoAtual,
                onValueChange = { textoAtual = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 24.sp, color = CorTexto),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirmarItem() }),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { confirmarItem() },
                modifier = Modifier
                    .size(56.dp)
                    .background(CorBotaoAdicionar, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Adicionar item",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Sugestões de autocompletar, logo abaixo do campo.
        if (sugestoes.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CorSugestaoFundo)
            ) {
                sugestoes.forEach { sugestao ->
                    Text(
                        text = sugestao,
                        fontSize = 22.sp,
                        color = CorTexto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { textoAtual = sugestao }
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaItem(numero: Int, texto: String, onApagar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "$numero.",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = CorTexto,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = texto,
            fontSize = 26.sp,
            color = CorTexto,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onApagar) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remover item",
                tint = CorLixeira,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
