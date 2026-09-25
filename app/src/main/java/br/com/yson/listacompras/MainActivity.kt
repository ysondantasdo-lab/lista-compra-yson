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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Verifica se o casal já digitou a senha com sucesso alguma vez
    val sharedPreferences = getSharedPreferences("AppCasalPrefs", Context.MODE_PRIVATE)
    val jaLogouAntes = sharedPreferences.getBoolean("casal_logado", false)

    // Verifica se o Firebase mantém o login anônimo ativo no celular
    val usuarioFirebaseExiste = FirebaseAuth.getInstance().currentUser != null

    setContent {
        MaterialTheme {
            // O app só pula o login se o casal já logou antes E o usuário do Firebase ainda existe
            var casalAtivo by remember { mutableStateOf(jaLogouAntes && usuarioFirebaseExiste) }

            if (casalAtivo) {
                ShoppingListScreen(
                    viewModel = viewModel,
                    onSair = {
                        viewModel.efetuarLogoutCompleto(onLogoutConcluido = {
                            casalAtivo = false
                        })
                    }
                )
            } else {
                LoginScreen(
                    viewModel = viewModel,
                    onEntrouComSucesso = {
                        // Quando o login der certo, salva no celular para não pedir de novo
                        sharedPreferences.edit().putBoolean("casal_logado", true).apply()
                        casalAtivo = true 
                    }
                )
            }
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
private val CorErro = Color(0xFFD32F2F)

@Composable
fun LoginScreen(viewModel: ShoppingListViewModel, onEntrouComSucesso: () -> Unit) {
    val context = LocalContext.current
    var senha by remember { mutableStateOf("") }
    var criandoNovoCodigo by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    fun confirmar() {
        if (senha.isBlank()) {
            mensagemErro = "Digite uma senha"
            return
        }
        mensagemErro = null
        carregando = true

        if (criandoNovoCodigo) {
            viewModel.criarNovoCasal(
                senha = senha,
                onSuccess = {
                    carregando = false
                    Toast.makeText(context, "Código criado! Guarde a senha: $senha", Toast.LENGTH_LONG).show()
                    onEntrouComSucesso()
                },
                onSenhaJaExiste = {
                    carregando = false
                    mensagemErro = "Essa senha já está em uso. Escolha outra."
                },
                onFailure = { e ->
                    carregando = false
                    mensagemErro = "Erro: ${e.message}"
                }
            )
        } else {
            viewModel.entrarEmCasalExistente(
                senha = senha,
                onSuccess = {
                    carregando = false
                    onEntrouComSucesso()
                },
                onNaoEncontrado = {
                    carregando = false
                    mensagemErro = "Senha não encontrada. Verifique ou crie um novo código."
                },
                onFailure = { e ->
                    carregando = false
                    mensagemErro = "Erro: ${e.message}"
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CorFundo)
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (criandoNovoCodigo) "Criar Novo Código" else "Entrar na Lista",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = CorTexto,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it; mensagemErro = null },
            label = { Text(if (criandoNovoCodigo) "Crie uma senha para o casal" else "Senha do casal") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { confirmar() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (mensagemErro != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensagemErro ?: "", color = CorErro, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { confirmar() },
            enabled = !carregando,
            colors = ButtonDefaults.buttonColors(containerColor = CorBotaoAdicionar),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(
                text = if (carregando) "Aguarde..." else if (criandoNovoCodigo) "Criar" else "Entrar",
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (criandoNovoCodigo) "Já tenho uma senha, quero entrar" else "Quero criar um novo código para nós",
            color = CorBotaoAdicionar,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable {
                    criandoNovoCodigo = !criandoNovoCodigo
                    mensagemErro = null
                }
                .padding(8.dp)
        )
    }
}

@Composable
    fun ShoppingListScreen(
        viewModel: ShoppingListViewModel,
        onSair: () -> Unit
    ) {
    val itens by viewModel.itens.collectAsState()
    val itensConhecidos by viewModel.itensConhecidos.collectAsState()
    val idCasal by viewModel.idCasal.collectAsState()

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
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {

        // Topo: código do casal (à esquerda, com espaço garantido) e botão de Sair.
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (idCasal.isBlank()) "Carregando código..." else "Código do Casal: $idCasal",
                fontSize = 14.sp,
                color = CorTexto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Sair do App",
                color = CorLixeira,
                modifier = Modifier
                    .clickable {
                        FirebaseAuth.getInstance().signOut()
                        val context = viewModel.getApplication<android.app.Application>().applicationContext
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                    }
                    .padding(start = 8.dp)
            )
        }

        // Lista numerada dos itens já lançados.
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(itens, key = { _, item -> item.id }) { index, item ->
                LinhaItem(
                    numero = index + 1,
                    texto = item.nome,
                    onApagar = { viewModel.removerItem(item.id) }
                )
                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
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
