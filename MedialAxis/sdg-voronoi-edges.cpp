// standard includes
#include <iostream>
#include <fstream>
#include <cassert>
#include <string>

// define the kernel
#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>

typedef CGAL::Exact_predicates_inexact_constructions_kernel Kernel;

// typedefs for the traits and the algorithm
#include <CGAL/Segment_Delaunay_graph_traits_2.h>
#include <CGAL/Segment_Delaunay_graph_2.h>

typedef CGAL::Segment_Delaunay_graph_traits_2<Kernel>  Gt;
typedef CGAL::Segment_Delaunay_graph_2<Gt>             SDG2;

using namespace std;

int main(int argc, char** argv)
{
    //cout << "testing" << endl;
    string fileName = argv[1];
    //for (int i = 0; i < argc; ++i)
    //{
        //cout << argv[i] << endl;
    //}

  //ifstream ifs("data/sites3.cin");
  ifstream ifs(fileName);
  assert( ifs );

  SDG2          sdg;
  SDG2::Site_2  site;

  // read the sites from the stream and insert them in the diagram
  while ( ifs >> site ) { sdg.insert( site ); }

  ifs.close();

  // validate the diagram
  assert( sdg.is_valid(true, 1) );
  //cout << endl << endl;

  /*
  // now walk through the non-infinite edges of the segment Delaunay
  // graphs (which are dual to the edges in the Voronoi diagram) and
  // print the sites defining each Voronoi edge.
  //
  // Each oriented Voronoi edge (horizontal segment in the figure
  // below) is defined by four sites A, B, C and D.
  //
  //     \                     /
  //      \         B         /
  //       \                 /
  //     C  -----------------  D
  //       /                 \
  //      /         A         \
  //     /                     \
  //
  // The sites A and B define the (oriented) bisector on which the
  // edge lies whereas the sites C and D, along with A and B define
  // the two endpoints of the edge. These endpoints are the Voronoi
  // vertices of the triples A, B, C and B, A, D.
  // If one of these vertices is the vertex at infinity the string
  // "infinite vertex" is printed; the corresponding Voronoi edge is
  // actually a stright-line or parabolic ray.
  // The sites below are printed in the order A, B, C, D.
  */

  string inf_vertex("infinite vertex");
  char vid[] = {'A', 'B', 'C', 'D'};

  SDG2::Finite_faces_iterator fit = sdg.finite_faces_begin();

  SDG2::Finite_edges_iterator eit = sdg.finite_edges_begin();
  for (int k = 1; eit != sdg.finite_edges_end(); ++eit, ++k) {
    SDG2::Edge e = *eit;
    // get the vertices defining the Voronoi edge
    SDG2::Vertex_handle v[] = { e.first->vertex( sdg.ccw(e.second) ),
                                e.first->vertex( sdg.cw(e.second) ),
                                e.first->vertex( e.second ),
                                sdg.tds().mirror_vertex(e.first, e.second) };
    //cout << "--- Edge " << k << " ---" << endl;


    typename CGAL::Segment_Delaunay_graph_traits_2<Kernel>::Line_2              l;
    typename CGAL::Segment_Delaunay_graph_traits_2<Kernel>::Segment_2           s;
    typename CGAL::Segment_Delaunay_graph_traits_2<Kernel>::Ray_2               r;
    CGAL::Parabola_segment_2<Gt>                                               ps;

    CGAL::Object o = sdg.primal(e);

    //cout << "start of primal" << endl;

    //if (CGAL::assign(l, o))   cout << "Line " << l << endl;
    //if (CGAL::assign(s, o))   cout << "segment " << s << endl;
    //if (CGAL::assign(r, o))   cout << "Ray " << r << endl;
    //if (CGAL::assign(ps, o))  cout << "parabola from"  << ps.p1 << " to " << ps.p2 << " defined by " << ps.center() << " " << ps.line() << endl;

    //cout << "end of primal" << endl;


    if (CGAL::assign(l, o))   cout << std::setprecision(10) << "l " << l << endl;
    if (CGAL::assign(s, o))   cout << std::setprecision(10) << "s " << s << endl;
    if (CGAL::assign(r, o))   cout << std::setprecision(10) << "r " << r << endl;
    if (CGAL::assign(ps, o))  cout << std::setprecision(10) << "p "  << ps.p1 << " " << ps.p2 << " " << ps.center() << " " << ps.line() << endl;

    for (int i = 0; i < 4; i++) {
      // check if the vertex is the vertex at infinity; if yes, print
      // the corresponding string, otherwise print the site


      if ( sdg.is_infinite(v[i]) ) {
        //cout << vid[i] << ": " << inf_vertex << endl;
      } else {
        //cout << vid[i] << ": " << v[i]->site() << endl;
      }
    }
    //cout << endl;
  }

  return 0;
}
