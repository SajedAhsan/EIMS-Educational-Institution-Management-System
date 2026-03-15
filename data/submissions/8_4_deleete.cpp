#include<bits/stdc++.h>
using namespace std;
void insert(vector<int>& v, int val)
{
    v.push_back(val);
    int curr_idx = v.size() - 1;
    while(curr_idx != 0)
    {
        int par_idx = (curr_idx-1)/2;
        if(v[par_idx] < v[curr_idx])
        swap(v[par_idx], v[curr_idx]);
        else
        break;
        curr_idx = par_idx;
    }
}
void delete_at_heap(vector<int>& v)
{
    v[0] = v.back();
    v.pop_back();
    int curr_idx = 0;
    while(1)
    {
        int left_idx = curr_idx * 2 + 1;
        int right_idx = curr_idx * 2 + 2;
        int left_val = INT_MIN, right_val = INT_MIN;
        if(left_idx < v.size())
        left_val = v[left_idx];
        if(right_idx < v.size())
        right_val = v[right_idx];
        if(left_val >= right_val && left_val > v[curr_idx])
        {
            swap(v[curr_idx], v[left_idx]);
            curr_idx = left_idx;
        }
        else if(right_val > left_val && right_val > v[curr_idx])
        {
            swap(v[curr_idx], v[right_idx]);
            curr_idx = right_idx;
        }
        else
        {
            break;
        }
    }

}
void print(vector<int> v)
{
    for(int i:v)
    cout << i << " ";
}
int main()
{
    int n;cin >> n;
    vector<int> v;
    for(int i=0; i<n; i++)
    {
        int val;cin>>val;
        insert(v, val);
    }
    print(v);
    cout << endl;
    delete_at_heap(v);
    print(v);
}