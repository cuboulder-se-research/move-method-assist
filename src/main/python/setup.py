from setuptools import setup

setup(
    name='mm_analyser',
    version='1.0',
    description='MM assist analysis module',
    packages=['mm_analyser'],  # same as name
    install_requires=['click', 'numpy', 'matplotlib','pandas'],  # external packages as dependencies
)
