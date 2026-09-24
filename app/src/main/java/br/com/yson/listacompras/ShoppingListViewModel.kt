package br.com.yson.listacompras

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Toda a lógica do app fica aqui: login anônimo (só pra satisfazer as regras
 * do Firestore), criação/entrada em um "casal" usando uma senha que funciona
 * como código compartilhado, escuta em tempo real da lista e dos itens
 * já conhecidos (autocompletar).
 */
class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val casaisCollection = db.collection("casais")
    private val itensConhecidosCollection = db.collection("itens_conhecidos")

    // A "senha" digitada pelo usuário É o código do casal (mais simples: um só campo).
    private var idCasalLogado: String? = null

    private var escutaItensListener: ListenerRegistration? = null
    private var escutaItensConhecidosListener: ListenerRegistration? = null

    private val _itens = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val itens: StateFlow<List<ShoppingItem>> = _itens

    private val _itensConhecidos = MutableStateFlow<List<String>>(emptyList())
    val itensConhecidos: StateFlow<List<String>> = _itensConhecidos

    private val _idCasal = MutableStateFlow("")
    val idCasal: StateFlow<String> = _idCasal

    /**
     * Cria um novo casal com a senha/código informado.
     * Chama onSenhaJaExiste se esse código já estiver em uso por outro casal.
     */
    fun criarNovoCasal(
        senha: String,
        onSuccess: () -> Unit,
        onSenhaJaExiste: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        garantirLoginAnonimo { erroLogin ->
            if (erroLogin != null) {
                onFailure(erroLogin)
                return@garantirLoginAnonimo
            }

            casaisCollection.document(senha).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        onSenhaJaExiste()
                    } else {
                        casaisCollection.document(senha)
                            .set(hashMapOf("criadoEm" to FieldValue.serverTimestamp()))
                            .addOnSuccessListener {
                                entrarNoCasal(senha)
                                onSuccess()
                            }
                            .addOnFailureListener { e -> onFailure(e) }
                    }
                }
                .addOnFailureListener { e -> onFailure(e) }
        }
    }

    /**
     * Entra em um casal já existente usando a senha/código informado.
     * Chama onNaoEncontrado se esse código não existir.
     */
    fun entrarEmCasalExistente(
        senha: String,
        onSuccess: () -> Unit,
        onNaoEncontrado: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        garantirLoginAnonimo { erroLogin ->
            if (erroLogin != null) {
                onFailure(erroLogin)
                return@garantirLoginAnonimo
            }

            casaisCollection.document(senha).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        entrarNoCasal(senha)
                        onSuccess()
                    } else {
                        onNaoEncontrado()
                    }
                }
                .addOnFailureListener { e -> onFailure(e) }
        }
    }

    private fun garantirLoginAnonimo(onResult: (Exception?) -> Unit) {
        if (auth.currentUser != null) {
            onResult(null)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { e -> onResult(e) }
    }

    private fun entrarNoCasal(senha: String) {
        idCasalLogado = senha
        _idCasal.value = senha
        escutarMudancas(senha)
    }

    private fun escutarMudancas(idCasal: String) {
        // Cancela escutas anteriores se existirem para evitar duplicidade
        escutaItensListener?.remove()
        escutaItensConhecidosListener?.remove()

        escutaItensListener = db.collection("listas_compras")
            .document(idCasal)
            .collection("itens")
            .orderBy("ordem")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ShoppingViewModel", "Erro ao escutar itens: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _itens.value = snapshot.documents.mapNotNull { documento ->
                        documento.toObject(ShoppingItem::class.java)?.copy(id = documento.id)
                    }
                }
            }

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

        db.collection("listas_compras")
            .document(idCasal)
            .collection("itens")
            .add(novoItem)

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
}
