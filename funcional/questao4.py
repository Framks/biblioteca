"""Questão 4: menor elemento e média geométrica de reais positivos."""

from math import exp, fsum, log


def menor_e_media_geometrica(numeros):
    """Retorne uma tupla; exp(média dos logs) evita formar o produto."""
    return (
        min(numeros),
        numeros[0]
        if len(numeros) == 1
        else exp(fsum(map(log, numeros)) / len(numeros)),
    )


if __name__ == "__main__":
    print(*menor_e_media_geometrica(tuple(map(float, input().split()))))
