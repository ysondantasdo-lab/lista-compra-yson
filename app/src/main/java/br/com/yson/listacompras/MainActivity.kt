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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast


class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                                // Estado que controla se o usuário está logado ou não
                var usuarioLogado by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

                if (usuarioLogado) {
                    // Se estiver logado, monitora o ID do casal e exibe a lista de compras
                    LaunchedEffect(Unit) {
                        viewModel.inicializarEscutaDoCasal()
                    }
                    ShoppingListScreen(viewModel)
                } else {
                    // Se não estiver logado, exibe a tela para Entrar/Cadastrar
                    LoginScreen(
                        onLoginSucesso = { usuarioLogado = true }
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



@Composable
fun LoginScreen(onLoginSucesso: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var codigoCasal by remember { mutableStateOf("") }
    
    // Controla se está na aba de "Entrar" ou de "Criar Conta"
    var criandoConta by remember { mutableStateOf(false) } 
    // Controla se vai entrar em um grupo existente ou criar um novo no cadastro
    var entrarEmGrupoExistente by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CorFundo)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (criandoConta) "Criar Conta do Casal" else "Entrar na Lista",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = CorTexto,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // Opções extras que só aparecem se ele estiver criando uma conta nova
        if (criandoConta) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = entrarEmGrupoExistente, onClick = { entrarEmGrupoExistente = true })
                Text("Tenho o código do meu parceiro", color = CorTexto)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !entrarEmGrupoExistente, onClick = { entrarEmGrupoExistente = false })
                Text("Criar um novo código para nós", color = CorTexto)
            }

            if (entrarEmGrupoExistente) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = codigoCasal,
                    onValueChange = { codigoCasal = it.uppercase() },
                    label = { Text("Código do Casal (Ex: CASAL-ABCD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || senha.isBlank()) {
                    Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (criandoConta) {
                    // Lógica para cadastrar nova conta
                    val auth = FirebaseAuth.getInstance()
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid ?: return@addOnSuccessListener
                            
                            // Define o ID do casal (ou gera um novo ou usa o digitado)
                            val idFinalCasal = if (entrarEmGrupoExistente) {
                                codigoCasal
                            } else {
                                "CASAL-" + java.util.UUID.randomUUID().toString().substring(0, 4).uppercase()
                            }

                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val dadosUsuario = hashMapOf("uid" to uid, "email" to email, "id_casal" to idFinalCasal)
                            
                            db.collection("usuarios").document(uid).set(dadosUsuario)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Conta criada! Seu código é: $idFinalCasal", Toast.LENGTH_LONG).show()
                                    onLoginSucesso()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Lógica para fazer Login em conta existente
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, senha)
                        .addOnSuccessListener {
                            onLoginSucesso()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Erro ao entrar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CorBotaoAdicionar),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (criandoConta) "Cadastrar" else "Entrar", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (criandoConta) "Já tem conta? Entre por aqui" else "Não tem conta? Cadastre-se aqui",
            color = CorBotaoAdicionar,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable { criandoConta = !criandoConta }
                .padding(8.dp)
        )
    }
}


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
            .imePadding()
            .padding(16.dp)
    ) {
        
        // Topo com botão de Sair (Logout)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Sair do App",
                color = CorLixeira,
                modifier = Modifier
                    .clickable {
                        FirebaseAuth.getInstance().signOut()
                        val context = viewModel.getApplication<android.app.Application>().applicationContext
                        // Recarrega a atividade para voltar para a tela de Login
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                    }
                            .padding(8.dp)
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
