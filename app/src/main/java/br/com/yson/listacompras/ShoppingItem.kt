package br.com.yson.listacompras

/**
 * Representa um item da lista de compras.
 * O construtor sem argumentos e os valores padrão são exigidos
 * pelo Firestore para conseguir mapear o documento automaticamente.
 */
data class ShoppingItem(
    val id: String = "",
    val nome: String = "",
    val ordem: Long = 0L
)
