#include <iostream>
#include <fstream>
#include <cassert>
#include <string>
#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>
#include <CGAL/Segment_Delaunay_graph_traits_2.h>
#include <CGAL/Segment_Delaunay_graph_2.h>

// Kernel to be used
typedef CGAL::Exact_predicates_inexact_constructions_kernel Kernel;

// Segment-Delaunay graph types
typedef CGAL::Segment_Delaunay_graph_traits_2<Kernel>  Gt;
typedef CGAL::Segment_Delaunay_graph_2<Gt>             SDG2;
typedef SDG2::Site_2 Site;
typedef SDG2::Vertex_handle VertexHandle;

// Geometry types
typedef CGAL::Point_2<Kernel> Point;
typedef CGAL::Line_2<Kernel> Line;
typedef CGAL::Segment_2<Kernel> Segment;
typedef CGAL::Ray_2<Kernel> Ray;
typedef CGAL::Parabola_segment_2<Gt> ParabolicSegment;

using namespace std;

int main(int argc, char** argv)
{
	// can read/write from standard input/output or files
	ifstream* ifs = nullptr;
	ofstream* ofs = nullptr;
	for (int i = 0; i < argc; ++i) {
		string a(argv[i]);
		if (a == "-in") {
			ifs = new ifstream(argv[i + 1]);
		}
		else if (a == "-out") {
			ofs = new ofstream(argv[i + 1]);
		}
	}

	istream& in = (ifs == nullptr) ? cin : *ifs;
	ostream& out = (ofs == nullptr) ? cout : *ofs;

	SDG2          sdg;
	Site site;
	map<VertexHandle, string> map;

	// read the sites from the stream and insert them in the diagram
	// keep track of a mapping from handles to strings for reconstructing the mapping between arcs and the input
	int sitenum = 0;
	while (in >> site) {
		string sitenumstr = to_string(sitenum);
		sitenum++;

		if (site.is_point()) {
			VertexHandle handle = sdg.insert(site);
			map.emplace(handle, "p " + sitenumstr);
		}
		else {
			// assumes segment
			VertexHandle src = sdg.insert(site.source());
			map.emplace(src, "src " + sitenumstr);

			VertexHandle tar = sdg.insert(site.target());
			map.emplace(src, "tar " + sitenumstr);

			VertexHandle handle = sdg.insert(site);
			map.emplace(handle, "s " + sitenumstr);
		}
	}

	// write the diagram
	out << std::setprecision(10);

	for (SDG2::Finite_edges_iterator eit = sdg.finite_edges_begin(); eit != sdg.finite_edges_end(); ++eit) {
		SDG2::Edge e = *eit;

		VertexHandle A = e.first->vertex(sdg.ccw(e.second)); // site defining the arc in the VD
		VertexHandle B = e.first->vertex(sdg.cw(e.second)); // site defining the arc in the VD
		// VertexHandle C = e.first->vertex(e.second) // site limiting the bisector of (A,B) in the VD
		// VertexHandle D = sdg.tds().mirror_vertex(e.first, e.second); // site limiting the bisector of (A,B) in the VD

		// Print keys for the sites that this arc belongs to
		out << map.at(A) << " " << map.at(B);

		// Obtain the actual geometry and print information about it
		CGAL::Object o = sdg.primal(e);

		Line l;
		if (CGAL::assign(l, o))  out << " l " << l;

		Segment s;
		if (CGAL::assign(s, o))  out << " s " << s;

		Ray r;
		if (CGAL::assign(r, o))   out << " r " << r;

		ParabolicSegment ps;
		if (CGAL::assign(ps, o)) {
			std::vector<Point> vec;
			ps.generate_points(vec, 50000); // something large, we're only interested in the first and last point. For whatever reason, CGAL blocked access to the endpoints?
			out << " ps " << vec[0] << " " << vec[vec.size() - 1];
		}

		out << endl;
	}

	out.flush();

	if (ifs != nullptr) {
		ifs->close();
		delete ifs;
	}
	if (ofs != nullptr) {
		ofs->close();
		delete ofs;
	}

	return 0;
}
