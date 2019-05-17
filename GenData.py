import numpy as np
import sys
from scipy.sparse import random, coo_matrix
from scipy.sparse import csr_matrix


def writeTofile(filename, label, cx):
    s=""
    f = open(filename, "w")
    for row,col,v in zip(cx.row, cx.col, cx.data):
        val = int(v)
        s=str(row)+","+str(col)+","+str(val)+","+label+"\n"
        f.write(s)
    print(label+'-Matrix File Created!')
    f.close()

def genOnFly(filename, row, col, density, label):
    s=""
    f = open(filename, "w")
    N = int(row * col * density)
    print("Total Cell with values:" + str(N))
    for i in range(N):
        r = np.random.randint(low=0, high=(row-1), dtype=int)
        c = np.random.randint(low=0, high=(col-1), dtype=int)
        v = np.random.randint(low=0, high=100, dtype=int)
        s=str(r)+","+str(c)+","+str(v)+","+label+"\n"
        f.write(s)
        if i % 1000000 == 0:
            print("Number of Rows written:" + str(i))
    f.close()

def convertToInt(cx):
    cx *= 100
    cx = cx.astype(int)
    return csr_matrix(cx)

def makeSparseMatrix(row, col, label, density=0.3):
    print("Density for " + label +" = " + str(density))
    mat = random(row, col, density=density)
    mat_csr = convertToInt(mat)
    print(label+'-Matrix Created!')
    return mat, mat_csr

def genData(row, col, d):
    a, a_csr = makeSparseMatrix(row, col, 'A', density=d)
    writeTofile("part-DummyA"+str(row)+"x"+str(col)+"D"+str(d)+".txt", 'A', a)
    
    b, b_csr = makeSparseMatrix(col, row, 'B', density=d)
    writeTofile("part-DummyB"+str(col)+"x"+str(row)+"D"+str(d)+".txt", 'B', b)
    
    c_csr = a_csr.dot(b_csr)
    print('Matrix Multiplication Complete')
    c = coo_matrix(c_csr)
    writeTofile("part-DummyC"+str(row)+"x"+str(row)+"D"+str(d)+".txt", 'C', c)
    print('Writing Result Complete!')

if __name__ == "__main__":
    row = int(sys.argv[2])
    col = int(sys.argv[3])
    density = float(sys.argv[4])
    print("Matrix A Shape : " + str(row) + "x"+ str(col))
    print("Matrix B Shape : " + str(col) + "x"+ str(row))
    print("Density of Matrix : " + str(density))
    if sys.argv[1] != 'fly':
        genData(row,col, density)
    else:
        genOnFly("part-DummyA"+str(row)+"x"+str(col)+"D"+str(density)+".txt",row,col, density, 'A')
        genOnFly("part-DummyB"+str(col)+"x"+str(row)+"D"+str(density)+".txt",col,row, density, 'B')

