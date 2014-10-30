package jnt.scimark2;
/*This is a test*/
import enerj.lang.*;

/**
    LU matrix factorization. (Based on TNT implementation.)
    Decomposes a matrix A  into a triangular lower triangular
    factor (L) and an upper triangular factor (U) such that
    A = L*U.  By convnetion, the main diagonal of L consists
    of 1's so that L and U can be stored compactly in
    a NxN matrix.


*/
public class LU 
{
    /**
        Returns a <em>copy</em> of the compact LU factorization.
        (useful mainly for debugging.)

        @return the compact LU factorization.  The U factor
        is stored in the upper triangular portion, and the L
        factor is stored in the lower triangular portion.
        The main diagonal of L consists (by convention) of
        ones, and is not explicitly stored.
    */


    protected static @Approx double[] new_copy(@Approx double x[])
    {
        int N = x.length;
        @Approx double T[] = new @Approx double[N];
        for (int i=0; i<N; i++)
            T[i] = x[i];
        return T;
    }


    protected static @Approx double[][] new_copy(@Approx double A[][])
    {
        int M = A.length;
        int N = A[0].length;

        @Approx double T[][] = new @Approx double[M][N];

        for (int i=0; i<M; i++)
        {
            @Approx double Ti[] = T[i];
            @Approx double Ai[] = A[i];
            for (int j=0; j<N; j++)
                Ti[j] = Ai[j];
        }

        return T;
    }



    public static int[] new_copy(int x[])
    {
        int N = x.length;
        int T[] = new int[N];
        for (int i=0; i<N; i++)
            T[i] = x[i];
        return T;
    }

    protected static final void insert_copy(@Approx double B[][], @Approx double A[][])
    {
        int M = A.length;
        int N = A[0].length;

		int remainder = N & 3;		 // N mod 4;

        for (int i=0; i<M; i++)
        {
            @Approx double Bi[] = B[i];
            @Approx double Ai[] = A[i];
			for (int j=0; j<remainder; j++)
                Bi[j] = Ai[j];
            for (int j=remainder; j<N; j+=4)
			{
				Bi[j] = Ai[j];
				Bi[j+1] = Ai[j+1];
				Bi[j+2] = Ai[j+2];
				Bi[j+3] = Ai[j+3];
			}
		}
        
    }
    public @Approx double[][] getLU()
    {
        return new_copy(LU_);
    }

    /**
        Returns a <em>copy</em> of the pivot vector.

        @return the pivot vector used in obtaining the
        LU factorzation.  Subsequent solutions must
        permute the right-hand side by this vector.

    */
    public int[] getPivot()
    {
        return new_copy(pivot_);
    }
    
    /**
        Initalize LU factorization from matrix.

        @param A (in) the matrix to associate with this
                factorization.
    */
    public LU( @Approx double A[][] )
    {
        int M = A.length;
        int N = A[0].length;

        //if ( LU_ == null || LU_.length != M || LU_[0].length != N)
            LU_ = new @Approx double[M][N];

        insert_copy(LU_, A);

        //if (pivot_.length != M)
            pivot_ = new int[M];

        factor(LU_, pivot_);
    }

    /**
        Solve a linear system, with pre-computed factorization.

        @param b (in) the right-hand side.
        @return solution vector.
    */
    public @Approx double[] solve(@Approx double b[])
    {
        @Approx double x[] = new_copy(b);

        solve(LU_, pivot_, x);
        return x;
    }
    

/**
    LU factorization (in place).

    @param A (in/out) On input, the matrix to be factored.
        On output, the compact LU factorization.

    @param pivit (out) The pivot vector records the
        reordering of the rows of A during factorization.
        
    @return 0, if OK, nozero value, othewise.
*/
public static @Approx int factor(@Approx double[][] A, int pivot[])
{
 


    int N = A.length;
    int M = A[0].length;

    int minMN = Math.min(M,N);

    for (int j=0; j<minMN; j++)
    {
        // find pivot in column j and  test for singularity.

        int jp=j;
        
        @Approx double t = ApproxMath.abs(A[j][j]);
        for (int i=j+1; i<M; i++)
        {
            @Approx double ab = ApproxMath.abs(A[i][j]);
            if (Endorsements.endorse(ab > t))
            {
                jp = i;
                t = ab;
            }
        }
        
        pivot[j] = jp;

        // jp now has the index of maximum element 
        // of column j, below the diagonal

        if (Endorsements.endorse( A[jp][j] == 0 ))                 
            return 1;       // factorization failed because of zero pivot


        if (jp != j)
        {
            // swap rows j and jp
            @Approx double tA[] = A[j];
            A[j] = A[jp];
            A[jp] = tA;
        }

        if (j<M-1)                // compute elements j+1:M of jth column
        {
            // note A(j,j), was A(jp,p) previously which was
            // guarranteed not to be zero (Label #1)
            //
            @Approx double recp =  1.0 / A[j][j];

            for (int k=j+1; k<M; k++)
                A[k][j] *= recp;
        }


        if (j < minMN-1)
        {
            // rank-1 update to trailing submatrix:   E = E - x*y;
            //
            // E is the region A(j+1:M, j+1:N)
            // x is the column vector A(j+1:M,j)
            // y is row vector A(j,j+1:N)


            for (int ii=j+1; ii<M; ii++)
            {
                @Approx double Aii[] = A[ii];
                @Approx double Aj[] = A[j];
                @Approx double AiiJ = Aii[j];
                for (int jj=j+1; jj<N; jj++)
                  Aii[jj] -= AiiJ * Aj[jj];

            }
        }
    }

    return 0;
}


    /**
        Solve a linear system, using a prefactored matrix
            in LU form.


        @param LU (in) the factored matrix in LU form. 
        @param pivot (in) the pivot vector which lists
            the reordering used during the factorization
            stage.
        @param b    (in/out) On input, the right-hand side.
                    On output, the solution vector.
    */
    public static void solve(@Approx double[][] LU, int pvt[], @Approx double b[])
    {
        int M = LU.length;
        int N = LU[0].length;
        int ii=0;

        for (int i=0; i<M; i++)
        {
            int ip = pvt[i];
            @Approx double sum = b[ip];

            b[ip] = b[i];
            if (ii==0)
                for (int j=ii; j<i; j++)
                    sum -= LU[i][j] * b[j];
            else 
                if (Endorsements.endorse(sum == 0.0))
                    ii = i;
            b[i] = sum;
        }

        for (int i=N-1; i>=0; i--)
        {
            @Approx double sum = b[i];
            for (int j=i+1; j<N; j++)
                sum -= LU[i][j] * b[j];
            b[i] = sum / LU[i][i];
        }
    }               


    private @Approx double LU_[][];
    private int pivot_[];
}
