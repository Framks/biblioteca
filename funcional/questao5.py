"""Questão 5: soma exata dos quadrados de 1 até n."""


def soma_quadrados(n):
    """A fórmula fechada usa apenas inteiros e também vale para n = 0."""
    return n * (n + 1) * (2 * n + 1) // 6


if __name__ == "__main__":
    print(soma_quadrados(int(input())))
