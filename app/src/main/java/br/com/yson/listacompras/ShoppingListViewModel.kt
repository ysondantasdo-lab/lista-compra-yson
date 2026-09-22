package br.com.yson.listacompras

import android.app.Application

import androidx.lifecycle.ViewModel

import androidx.lifecycle.AndroidViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import android.util.Log
import java.util.UUID

import com.google.firebase.firestore.ListenerRegistration



/**
 * Toda a lógica do app fica aqui: login anônimo, escuta em tempo real
 * da lista compartilhada e escuta dos itens já conhecidos (autocompletar).
 */
class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    // Armazena o ID do casal logado dinamicamente
    private var idCasalLogado: String? = null

    // Guardam as escutas ativas do Firebase para poder cancelar no logout
    private var escutaItensListener: ListenerRegistration? = null
    private var escutaItensConhecidosListener: ListenerRegistration? = null

    /**
     * Busca o ID do casal vinculado ao usuário atual e inicia a sincronização da lista.
     */
    fun inicializarEscutaDoCasal() {
        val uidUsuarioLogado = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uidUsuarioLogado).get()
            .addOnSuccessListener { document ->
                val idCasal = document.getString("id_casal")
                if (idCasal != null) {
                    idCasalLogado = idCasal
                    escutarMudancas(idCasal)
                } else {
                    Log.e("ShoppingViewModel", "Usuário não possui ID de casal associado.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingViewModel", "Erro ao buscar usuário: ${e.message}")
            }
    }


    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val itensCollection = db.collection("lista_compras")
    private val itensConhecidosCollection = db.collection("itens_conhecidos")

    private val _itens = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val itens: StateFlow<List<ShoppingItem>> = _itens

    private val _itensConhecidos = MutableStateFlow<List<String>>(emptyList())
    val itensConhecidos: StateFlow<List<String>> = _itensConhecidos


    /**
     * OPÇÃO 1: Cadastra o primeiro membro do casal e CRIA um novo grupo (ID do Casal)
     */
    fun cadastrarECriarNovoCasal(email: String, senha: String, onSuccess: (idCasalGerado: String) -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                
                // Gera um código único e curto para o casal (Ex: CASAL-A8F2)
                val idCasalNovo = "CASAL-" + UUID.randomUUID().toString().substring(0, 4).uppercase()

                val novoUsuario = Usuario(uid = uid, email = email, id_casal = idCasalNovo)

                // Salva o usuário na coleção "usuarios" vinculando ao ID criado
                db.collection("usuarios").document(uid).set(novoUsuario)
                    .addOnSuccessListener {
                        onSuccess(idCasalNovo)
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }


    /**
     * OPÇÃO 2: Cadastra o parceiro e o VINCULA a um código de grupo existente (enviado pelo cônjuge)
     */
    fun cadastrarEEntrarEmCasalExistente(email: String, senha: String, idCasalExistente: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                val novoUsuario = Usuario(uid = uid, email = email, id_casal = idCasalExistente)

                // Salva o parceiro apontando para o mesmo id_casal
                db.collection("usuarios").document(uid).set(novoUsuario)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}


    private fun escutarMudancas(idCasal: String) {
        // Cancela escutas anteriores se existirem para evitar duplicidade
        escutaItensListener?.remove()
        escutaItensConhecidosListener?.remove()

        // 1. Busca a lista de compras privada dentro da subcoleção do casal
        escutaItensListener = db.collection("listas_compras")
            .document(idCasal)
            .collection("itens")
            .orderBy("ordem")
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    _itens.value = snapshot.documents.mapNotNull { documento ->
                        documento.toObject(ShoppingItem::class.java)?.copy(id = documento.id)
                    }
                }
            }

        // 2. Busca os nomes já sugeridos antes (Mantido global para ajudar no autocompletar geral)
        escutaItensConhecidosListener = itensConhecidosCollection.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                _itensConhecidos.value = snapshot.documents.mapNotNull { it.getString("nome") }
            }
        }
    }

    fun adicionarItem(nome: String) {
        val idCasal = idCasalLogado ?: return
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) return

        val proximaOrdem = (_itens.value.maxOfOrNull { it.ordem } ?: 0L) + 1L

        val novoItem = hashMapOf(
            "nome" to nomeLimpo,
            "ordem" to proximaOrdem
        )

        // Salva na subcoleção privada do casal
        db.collection("listas_compras")
            .document(idCasal)
            .collection("itens")
            .add(novoItem)

        // Guarda o nome globalmente para sugerir no autocompletar
        itensConhecidosCollection.document(nomeLimpo.lowercase())
            .set(hashMapOf("nome" to nomeLimpo, "atualizadoEm" to FieldValue.serverTimestamp()))
    }

    fun removerItem(id: String) {
        val idCasal = idCasalLogado ?: return
        
        db.collection("listas_compras")
            .document(idCasal)
            .collection("itens")
            .document(id)
            .delete()
    }

    override fun onCleared() {
        super.onCleared()
        escutaItensListener?.remove()
        escutaItensConhecidosListener?.remove()
    }


