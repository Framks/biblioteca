"""Questão 1: produto dos inteiros estritamente positivos."""

from math import prod


def produto_positivos(numeros):
    """Ignore zeros e negativos; o produto vazio vale 1."""
    return prod(filter(lambda numero: numero > 0, numeros))


if __name__ == "__main__":
    print(produto_positivos(map(int, input().split())))
