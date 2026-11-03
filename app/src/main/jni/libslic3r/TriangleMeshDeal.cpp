#include "TriangleMeshDeal.hpp"

#include <algorithm>
#include <cstddef>
#include <limits>
#include <utility>
#include <igl/loop.h>
#include "Point.hpp"
#undef NDEBUG
#include <assert.h>
#include <boost/log/trivial.hpp>

namespace Slic3r {
TriangleMesh TriangleMeshDeal::smooth_triangle_mesh(const TriangleMesh& mesh, bool& ok, TriangleMesh* unsmoothed)
{
    ok = false;
    const size_t vertices_count = mesh.its.vertices.size();
    const size_t indices_count = mesh.its.indices.size();
    if (vertices_count < 3 || indices_count == 0 || indices_count > std::numeric_limits<size_t>::max() / 4)
        return {};
    for (const auto& face : mesh.its.indices) {
        if (face(0) < 0 || face(1) < 0 || face(2) < 0 ||
            static_cast<size_t>(face(0)) >= vertices_count || static_cast<size_t>(face(1)) >= vertices_count ||
            static_cast<size_t>(face(2)) >= vertices_count)
            return {};
    }
    {
        using namespace igl;
        typedef Eigen::Matrix<float, Eigen::Dynamic, 3, Eigen::DontAlign | Eigen::RowMajor> RowMatrixX3f;
        typedef Eigen::Matrix<int, Eigen::Dynamic, 3, Eigen::DontAlign | Eigen::RowMajor>   RowMatrixX3i;

        // igl::loop requires owning Eigen matrices (the bundled libigl version does not
        // accept Eigen::Map as its PlainObjectBase input type).
        RowMatrixX3f OV(static_cast<Eigen::Index>(vertices_count), 3);
        RowMatrixX3i OF(static_cast<Eigen::Index>(indices_count), 3);
        for (size_t i = 0; i < vertices_count; ++i)
            OV.row(static_cast<Eigen::Index>(i)) = mesh.its.vertices[i];
        for (size_t i = 0; i < indices_count; ++i)
            OF.row(static_cast<Eigen::Index>(i)) = mesh.its.indices[i];
        Eigen::MatrixX3f                     V;
        Eigen::MatrixX3i                     F;

        loop(OV, OF, V, F);

        if (V.rows() <= 0 || F.rows() != static_cast<Eigen::Index>(indices_count * 4))
            return {};

        indexed_triangle_set its;
        its.vertices.reserve(static_cast<size_t>(V.rows()));
        its.indices.reserve(static_cast<size_t>(F.rows()));
        for (Eigen::Index i = 0; i < V.rows(); ++i)
            its.vertices.emplace_back(V(i, 0), V(i, 1), V(i, 2));
        for (Eigen::Index i = 0; i < F.rows(); ++i)
            its.indices.emplace_back(F(i, 0), F(i, 1), F(i, 2));
        if (unsmoothed) {
            // igl::loop keeps each input vertex index and replaces each triangle by four.
            indexed_triangle_set flat;
            flat.indices = its.indices;
            flat.vertices.assign(its.vertices.size(), Vec3f::Zero());
            std::copy(mesh.its.vertices.begin(), mesh.its.vertices.end(), flat.vertices.begin());
            for (size_t i = 0; i < indices_count; ++i) {
                const stl_triangle_vertex_indices& face = mesh.its.indices[i];
                const Vec3f& v0 = mesh.its.vertices[face(0)];
                const Vec3f& v1 = mesh.its.vertices[face(1)];
                const Vec3f& v2 = mesh.its.vertices[face(2)];
                flat.vertices[its.indices[4 * i](1)] = 0.5f * (v0 + v1);
                flat.vertices[its.indices[4 * i + 1](1)] = 0.5f * (v1 + v2);
                flat.vertices[its.indices[4 * i](2)] = 0.5f * (v2 + v0);
            }
            *unsmoothed = TriangleMesh(std::move(flat));
        }
        ok = true;
        TriangleMesh result_mesh(its);
        return result_mesh;
    }
}
} // namespace Slic3r
