#include<iostream>
#include<string.h>
#include<stdlib.h>
using namespace std;
class Figure{
    int row, col;
    int **mat;
    char *name;
    void set_mat(int r, int c){
        mat = new int*[r];
        for(int i=0; i<r; i++){
            mat[i] = new int[c];
        }
    }
    public:
        //parameter chara constructor
        Figure(){
            row = 0;
            col = 0;
            name = new char[10];
            strcpy(name, "Zero0D");
            mat = NULL;

            cout << "Object created without any parameter\n";
        }

        // parameter er constructor
        Figure(int r, int c, int *ar){
            row = r, col = c;
            set_mat(r, c);
            int curr_idx = 0;
            for(int i=0; i<r; i++){
                for(int j=0; j<c;j++){
                    mat[i][j] = ar[curr_idx++];
                }
            }
            char shape[20], dim[5];
            sprintf(dim,  "%dD", c);
            if(r==2) strcpy(shape, "Line");
            else if(r==3) strcpy(shape, "Triangle");
            else strcpy(shape, "Rectangle");
            strcat(shape, dim);
            name = new char[strlen(shape) + 1];
            strcpy(name, shape);

            cout << name << " Object created successfully\n";
        }

        // copy constructor
        Figure(const Figure &f){
            row = f.row;
            col = f.col;
            name = new char[strlen(f.name) + 1];
            strcpy(name, f.name);
            if(f.mat == NULL)mat = NULL;
            else{
                set_mat(f.row, f.col);
                for(int i=0; i<row; i++)
                    for(int j=0; j<col; j++)
                        mat[i][j] = f.mat[i][j];
            }
            cout << "Object created via deep copying\n";
        }

        //destructor function
        ~Figure(){
            if(mat != NULL){
                for(int i=0; i<row; i++){
                    for(int j=0; j<col; j++){
                        mat[i][j] = 0;
                        cout << mat[i][j] << " ";
                    }
                    cout << "\n";
                }
                cout << "Matrix of "<< name << " has been set to 0\n";
                for(int i=0; i<row; i++)
                    delete[] mat[i];
                delete[] mat;
            }

            cout << name << " has been destroyed\n\n";
            row = 0, col = 0;
            delete[] name;
        }
        
        void show(){
            cout << "Object Shape with dimension : " << name << "\n";
            
            if(mat!=NULL){
                cout << "Printing the co ordinates of points of the object : \n";
                for(int i=0; i<row; i++){
                    for(int j=0; j<col; j++){
                        cout << mat[i][j] << " ";
                    }
                    cout << "\n";
                }
            }
            else cout << "The object has 0 dimension\n";
            cout << "Object shown fully\n";
        }

        int getsum(){
            if(mat == NULL) return 0;
            int sum=0;
            for(int i=0; i<row; i++)
                for(int j=0; j<col; j++)
                    sum += mat[i][j];
            
            return sum;
        }

        int getsum(int m, int n){
            if(mat == NULL) return 0;

            if(m>row || n>col){
                cout << "ERROR\n";
                exit(1);
            }
            int sum=0;
            for(int i=0; i<m; i++)
                for(int j=0; j<n; j++)
                    sum += mat[i][j];

            return sum;
        }

};
int main(){
    cout << "Welcome to constructor-destructor showcasing programme:\n";
    int cmnd = -1;
    do{
        cout << "Enter:\n1 -> For continuing operation\n0 -> For terminating the programme\n";
        cin >> cmnd;
        if(cmnd == 1){
            int r, c;
            cout << "Enter number of point: ";
            cin >> r;
            cout << "\nEnter number of coordinates: ";
            cin >> c;
            int ar[r * c];
            cout << "\nGive the coordinates of the points in a single line:\n";
            for(int i=0; i<r*c; i++)
                cin >> ar[i];
    
            Figure f1(r, c, ar);
            f1.show();
            cout << "Total sum : " << f1.getsum() << "\n";

            int m, n;
            cout << "Give number of rows and columns for showing partial sum:\n";
            cin >> m >> n;
            cout << "Partial sum upto row " <<m<<" col "<<n << " : " << f1.getsum(m,n) << "\n";

            cout << "\n\nAnother object is now being created via copy constructor\n";
            Figure f2(f1);
            cout << "\n\n";
            f2.show();

            cout << "\n\nCreating an object without any parameter\n";
            Figure f3;
            cout << "\n\n";
            f3.show();

            cout << "\n\n";
        }
        else if(cmnd == 0){
            cout << "Terminating the programme\n";
        }
        else{
            cout << "Wrong command\n";
        }
    }while(cmnd!=0);
    return 0;
}